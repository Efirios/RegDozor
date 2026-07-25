package org.regdozor.telegram;

import org.regdozor.store.SeenStore;

import java.util.Set;

/**
 * «Рассыльщик»: знает КОМУ. Берёт всех подписчиков из реестра (chat-ids.txt) и шлёт каждому
 * один и тот же текст через TelegramNotifier (тот знает только КАК отправить одному названному чату).
 * Так «кому» и «как» разведены по разным классам.
 *
 * Отправка каждому обёрнута в try/catch: если один получатель отвалился (напр. заблокировал бота),
 * он НЕ должен обрывать рассылку остальным. Осознанный размен: лучше потерять одно сообщение
 * одному, чем не отправить никому. Список «доставленных» не ведём — уведомление одноразовое.
 */
public class Broadcaster {
    /** Реестр подписчиков — множество chat_id из файла. */
    private final SeenStore seenStore;
    /** «Оповещатель»: шлёт текст в ОДИН названный чат. */
    private final TelegramNotifier telegramNotifier;

    public Broadcaster(SeenStore seenStore, TelegramNotifier telegramNotifier) {
        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (telegramNotifier == null) {
            throw new IllegalArgumentException("telegramNotifier не может быть null!");
        }
        this.telegramNotifier = telegramNotifier;
    }

    /**
     * Шлёт один и тот же текст ВСЕМ подписчикам из реестра.
     * Реестр читается на каждый вызов — значит подписавшиеся только что тоже получат.
     *
     * @param text готовое сообщение (уже с HTML-разметкой Telegram)
     */
    public void broadcast(String text) {
        Set<String> chatIds = seenStore.load();
        for (String chatId : chatIds) {
            try {
                telegramNotifier.send(chatId, text);
            } catch (RuntimeException e) {
                System.out.println("Не удалось отправить сообщение chatId=" + chatId + ": " + e.getMessage());
            }
        }
    }
}
