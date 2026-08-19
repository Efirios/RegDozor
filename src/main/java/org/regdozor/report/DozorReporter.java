package org.regdozor.report;

import org.jsoup.nodes.Document;
import org.regdozor.match.KoapRisk;
import org.regdozor.match.RiskMemo;
import org.regdozor.operator.ArticleTextFetcher;
import org.regdozor.match.RelevanceStrategy;
import org.regdozor.profile.Profile;
import org.regdozor.profile.ProfileStore;
import org.regdozor.profile.UserProduct;
import org.regdozor.store.SeenStore;
import org.regdozor.telegram.TelegramNotifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Проверяет ОДИН документ и, если он касается товаров пользователя, шлёт уведомление.
 *
 * Координатор «дозора», и ТОЛЬКО координатор: держит порядок шагов, а каждое решение делегирует.
 * Сам он не знает ни про источники, ни про их разметку, ни про текст сообщений — поэтому один класс
 * обслуживает и ленту честныйзнака, и статьи markirovka.
 *
 * Две оси различия между источниками вынесены в стратегии, обе подставляются при сборке в {@code App}:
 * <ul>
 *   <li>{@link RelevanceStrategy} — «документ вообще про клиента?» (по кодам ИЛИ по названию группы);</li>
 *   <li>{@link AlertStrategy} — «как ему об этом рассказать?» (пер-товарный алерт с этапами и риском
 *       ИЛИ короткое «вышел релиз по твоей группе»).</li>
 * </ul>
 *
 * 🚨 ИДЁТ ЦИКЛОМ ПО ПОДПИСЧИКАМ, у каждого СВОЙ профиль. Раньше профиль был один на всех и лежал
 * ресурсом внутри JAR — подписчик-юрлицо получал бы суммы штрафов, рассчитанные для ИП, то есть
 * заниженные в 30 раз. Теперь профиль приходит из {@code ProfileStore} по chat_id и передаётся
 * стратегиям ПАРАМЕТРОМ: одна и та же стратегия обслуживает разных людей, поэтому держать профиль
 * в поле нельзя.
 *
 * Отсюда и доставка: сообщения у разных подписчиков РАЗНЫЕ, поэтому {@code TelegramNotifier.send}
 * каждому лично, а не {@code Broadcaster.broadcast} «всем одинаковое».
 *
 * ⚠️ ПОРЯДОК В {@code run} НЕ ПЕРЕСТАВЛЯТЬ, на нём держится вся экономия:
 * <ul>
 *   <li>страница, её текст и {@link RiskMemo} — ДО цикла, по одному разу на документ. Внутри цикла
 *       это превратилось бы в N скачиваний и N чтений кодекса;</li>
 *   <li>проверка «никого не задело» — ДО сборки сообщения. Собирать текст раньше — работа впустую,
 *       а на markirovka-пути ещё и поход в сеть за КоАП (~6.5 МБ) ради сообщения, которое не уйдёт.
 *       Проверено: на статье, которая клиента не касается, расчётов риска НОЛЬ.</li>
 * </ul>
 *
 * ⚠️ Сбой у ОДНОГО подписчика не должен обрывать рассылку остальным — отсюда {@code try/catch}
 * внутри цикла. Тот же осознанный размен, что в {@code Broadcaster}: лучше потерять одно сообщение
 * одному, чем не отправить никому.
 *
 * ⚠️ Подписчик БЕЗ профиля — это норма, а не поломка: человек нажал Start, но онбординг не проходил.
 * Такого пропускаем с записью в лог, а не падаем.
 */
public class DozorReporter {
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceStrategy relevanceStrategy;
    private final TelegramNotifier telegramNotifier;
    private final AlertStrategy alertStrategy;
    private final SeenStore seenStore;
    private final ProfileStore profileStore;
    private final KoapRisk koapRisk;

    public DozorReporter(ArticleTextFetcher articleTextFetcher,
                         RelevanceStrategy relevanceStrategy, TelegramNotifier telegramNotifier, AlertStrategy alertStrategy, SeenStore seenStore, ProfileStore profileStore, KoapRisk koapRisk) {
        if (articleTextFetcher == null) {
            throw new IllegalArgumentException("articleTextFetcher не может быть null!");
        }
        this.articleTextFetcher = articleTextFetcher;

        if (relevanceStrategy == null) {
            throw new IllegalArgumentException("relevanceStrategy не может быть null!");
        }
        this.relevanceStrategy = relevanceStrategy;

        if (telegramNotifier == null) {
            throw new IllegalArgumentException("telegramNotifier не может быть null!");
        }
        this.telegramNotifier = telegramNotifier;

        if (alertStrategy == null) {
            throw new IllegalArgumentException("alertStrategy не может быть null!");
        }
        this.alertStrategy = alertStrategy;

        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (profileStore == null) {
            throw new IllegalArgumentException("profileStore не может быть null!");
        }
        this.profileStore = profileStore;

        if (koapRisk == null) {
            throw new IllegalArgumentException("koapRisk не может быть null!");
        }
        this.koapRisk = koapRisk;
    }

    /**
     * Обрабатывает ОДИН документ: скачать страницу → извлечь текст → и дальше НА КАЖДОГО ПОДПИСЧИКА:
     * его профиль → его матч → его сообщение → отправка ему лично. Кого документ не касается —
     * тому молчим (сообщения, которого не должно быть, не создаём).
     *
     * Страница качается ОДИН раз на документ, а не на подписчика: {@code Document} получают и
     * извлекатель текста (для матча), и стратегия сообщения (markirovka-реализация читает по нему
     * раздел этапов). Заодно это гарантирует, что матч и цитата сделаны по ОДНОЙ версии страницы.
     *
     * @param documentUrl ссылка на документ — выпуск из ленты честныйзнака либо статья markirovka,
     *                    смотря какой монитор позвал
     */
    public void run(String documentUrl) {
        Document doc = articleTextFetcher.fetchDocument(documentUrl);
        String text = articleTextFetcher.extractText(doc);
        RiskMemo riskMemo = new RiskMemo(koapRisk);

        Set<String> chatIds = seenStore.load();
        for (String chatId : chatIds) {
            try {
                Optional<Profile> profile = profileStore.load(chatId);
                if (profile.isEmpty()) {
                    System.out.println("Такого профиля не существует: " + chatId);
                    continue;
                }

                Profile profileGet = profile.get();

                List<UserProduct> relevant = relevanceStrategy.findRelevant(text, profileGet);
                if (relevant.isEmpty()) {
                    continue;
                }

                String alert = alertStrategy.composeAlert(doc, documentUrl, profileGet, relevant, riskMemo);
                telegramNotifier.send(chatId, alert);
            } catch (RuntimeException e) {
                System.out.println("Не удалось обработать подписчика: " + chatId + ": " + e.getMessage());
            }
        }
    }
}
