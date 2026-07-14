package org.regdozor;

import java.util.Set;

/**
 * Координатор захвата подписчиков: опрашивает Telegram (getUpdates через TelegramReceiver),
 * записывает chat_id написавших в реестр и встречает новичков (через OnboardingReporter).
 * Крутится на планировщике раз в несколько секунд (long polling).
 *
 * ⚠️ КЛЮЧЕВОЕ УСТРОЙСТВО run() — не менять порядок, тут два урока:
 * 1) Первый цикл ТОЛЬКО регистрирует (никакой сети!), после него реестр и offset сохраняются ВСЕГДА.
 *    Если бы отправка стояла в этом цикле и упала, save() не выполнились бы, offset не сдвинулся,
 *    то же обновление вернулось бы снова — и один заблокировавший бота парализовал бы ВСЮ очередь
 *    (livelock: другие новые подписчики никогда бы не прошли).
 * 2) Онбординг вынесен во ВТОРОЙ цикл и опирается на ОТДЕЛЬНЫЙ список welcomed («кому уже отправили»).
 *    Реестр отвечает «кого знаем», welcomed — «кому доставили»: это разные факты.
 *    Пометка welcomed.add() стоит ВНУТРИ try СРАЗУ ПОСЛЕ onboard() — значит она выполнится, только если
 *    отправка прошла. Упало (моргнул VPN) → не пометили → на следующем тике повторим. Само чинится.
 */
public class SubscriberMonitor {
    /** «Приёмник»: спрашивает у Telegram новые обновления. */
    private final TelegramReceiver telegramReceiver;
    /** Память на одно число: граница «обработано до сюда» (tg-offset.txt). */
    private final OffsetStore offsetStore;
    /** Реестр подписчиков: «кого мы знаем» (chat-ids.txt). */
    private final SeenStore seenStore;
    /** «Встречающий»: приветствие + baseline-карточки новичку. */
    private final OnboardingReporter onboardingReporter;
    /** Второй список: «кому онбординг УЖЕ успешно доставлен» (welcomed.txt). Не путать с реестром! */
    private final SeenStore welcomedStore;

    public SubscriberMonitor(TelegramReceiver telegramReceiver, OffsetStore offsetStore, SeenStore seenStore,
                             OnboardingReporter onboardingReporter, SeenStore welcomedStore) {
        if (telegramReceiver == null) {
            throw new IllegalArgumentException("telegramReceiver не может быть null!");
        }
        this.telegramReceiver = telegramReceiver;

        if (offsetStore == null) {
            throw new IllegalArgumentException("offsetStore не может быть null!");
        }
        this.offsetStore = offsetStore;

        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (onboardingReporter == null) {
            throw new IllegalArgumentException("onboardingReporter не может быть null!");
        }
        this.onboardingReporter = onboardingReporter;

        if (welcomedStore == null) {
            throw new IllegalArgumentException("welcomedStore не может быть null!");
        }
        this.welcomedStore = welcomedStore;
    }

    public void run() {
        long offset = offsetStore.load();
        GetUpdatesResponse getUpdatesResponse = telegramReceiver.receive(offset);
        Set<String> chatIds = seenStore.load();
        long maxUpdateId = offset - 1;

        for (Update update : getUpdatesResponse.result()) {
            if (update.message() != null) {
                chatIds.add(String.valueOf(update.message().chat().id()));
            }

            if (update.update_id() > maxUpdateId) {
                maxUpdateId = update.update_id();
            }
        }

        seenStore.save(chatIds);
        offsetStore.save(maxUpdateId + 1);

        Set<String> welcomed = welcomedStore.load();

        for (String chatId : chatIds) {
            if (!welcomed.contains(chatId)) {
                try {
                    onboardingReporter.onboard(chatId);
                    welcomed.add(chatId);
                } catch (RuntimeException e) {
                    System.out.println("Не удалось поприветствовать chatId=" + chatId + ": " + e.getMessage());
                }
            }
        }

        welcomedStore.save(welcomed);
    }
}
