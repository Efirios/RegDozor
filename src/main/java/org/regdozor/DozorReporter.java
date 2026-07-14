package org.regdozor;

import java.util.List;

/**
 * Координатор "дозора" для ОДНОГО документа: run(url) скачивает чистый текст и через внедрённую
 * СТРАТЕГИЮ ({@link RelevanceStrategy} — по кодам ИЛИ по группе, решается при сборке в App) проверяет,
 * относится ли документ к товарам/группе пользователя.
 * Если да — шлёт КОРОТКОЕ уведомление: факт «вышел релиз по твоей группе» + ссылка на первоисточник + дисклеймер.
 * БЕЗ карточек с риском: группа-матч знает лишь, что релиз упоминает группу, но НЕ про что он именно, —
 * поэтому конкретику (обязанности/штрафы) не приписываем непрочитанному релизу, она живёт в baseline.
 * Если не релевантно — молчит (сообщения, которого не должно быть, не создаём).
 */
public class DozorReporter {
    private final ProductLoader productLoader;
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceStrategy relevanceStrategy;
    private final Broadcaster broadcaster;

    public DozorReporter(ProductLoader productLoader, ArticleTextFetcher articleTextFetcher,
                         RelevanceStrategy relevanceStrategy, Broadcaster broadcaster) {
        if (productLoader == null) {
            throw new IllegalArgumentException("productLoader не может быть null!");
        }
        this.productLoader = productLoader;

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
    }

    public void run(String documentUrl) {
        Product[] products = productLoader.load();
        String text = articleTextFetcher.fetchCleanText(documentUrl);
        List<Product> relevant = relevanceStrategy.findRelevant(text, products);

        if (relevant.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🔔 <b>ЦРПТ опубликовал новый релиз по твоей товарной группе</b>\n");
        sb.append("Что именно изменилось — смотри первоисточник.\n\n");
        sb.append("📄 <a href=\"").append(documentUrl).append("\">Первоисточник</a>\n\n");
        sb.append("\n⚠\uFE0F ").append(AlertFormatter.DISCLAIMER);
        broadcaster.broadcast(sb.toString());
    }
}
