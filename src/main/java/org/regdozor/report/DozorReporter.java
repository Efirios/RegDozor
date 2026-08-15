package org.regdozor.report;

import org.jsoup.nodes.Document;
import org.regdozor.operator.ArticleTextFetcher;
import org.regdozor.match.RelevanceStrategy;
import org.regdozor.profile.Profile;
import org.regdozor.profile.UserProduct;
import org.regdozor.telegram.Broadcaster;

import java.util.List;

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
 * Товары берёт из ПРОФИЛЯ (внедрён готовым, грузится один раз в App) — раньше на КАЖДЫЙ документ
 * перечитывался каталог, это ушло вместе с переездом кодов в профиль.
 *
 * ⚠️ ПОРЯДОК В {@code run} НЕ ПЕРЕСТАВЛЯТЬ: проверка «никого не задело» стоит ДО сборки сообщения.
 * Собирать текст раньше — работа впустую, а на markirovka-пути ещё и поход в сеть за текстом КоАП
 * (~6.5 МБ) ради сообщения, которое никто не отправит.
 */
public class DozorReporter {
    private final Profile profile;
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceStrategy relevanceStrategy;
    private final Broadcaster broadcaster;
    private final AlertStrategy alertStrategy;

    public DozorReporter(Profile profile, ArticleTextFetcher articleTextFetcher,
                         RelevanceStrategy relevanceStrategy, Broadcaster broadcaster, AlertStrategy alertStrategy) {
        if (profile == null) {
            throw new IllegalArgumentException("profile не может быть null!");
        }
        this.profile = profile;

        if (articleTextFetcher == null) {
            throw new IllegalArgumentException("articleTextFetcher не может быть null!");
        }
        this.articleTextFetcher = articleTextFetcher;

        if (relevanceStrategy == null) {
            throw new IllegalArgumentException("relevanceStrategy не может быть null!");
        }
        this.relevanceStrategy = relevanceStrategy;

        if (broadcaster == null) {
            throw new IllegalArgumentException("broadcaster не может быть null!");
        }
        this.broadcaster = broadcaster;

        if (alertStrategy == null) {
            throw new IllegalArgumentException("alertStrategy не может быть null!");
        }
        this.alertStrategy = alertStrategy;
    }

    /**
     * Обрабатывает ОДИН документ: скачать → извлечь текст → проверить релевантность → собрать
     * сообщение стратегией → разослать всем. Если документ не про пользователя — молчит
     * (сообщения, которого не должно быть, не создаём).
     *
     * Страница качается ОДИН раз: {@code Document} получают и извлекатель текста (для матча),
     * и стратегия сообщения (markirovka-реализация читает по нему раздел этапов). Заодно это
     * гарантирует, что матч и цитата сделаны по ОДНОЙ версии страницы.
     *
     * @param documentUrl ссылка на документ — выпуск из ленты честныйзнака либо статья markirovka,
     *                    смотря какой монитор позвал
     */
    public void run(String documentUrl) {
        Document doc = articleTextFetcher.fetchDocument(documentUrl);
        String text = articleTextFetcher.extractText(doc);
        List<UserProduct> relevant = relevanceStrategy.findRelevant(text, profile.products());

        if (relevant.isEmpty()) return;

        String alert = alertStrategy.composeAlert(doc, documentUrl, relevant);
        broadcaster.broadcast(alert);
    }
}
