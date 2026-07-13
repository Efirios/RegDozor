package org.regdozor;

import java.util.List;

/**
 * Координатор "дозора" для ОДНОГО документа: run(url) скачивает чистый текст,
 * находит релевантные товары через внедрённую СТРАТЕГИЮ ({@link RelevanceStrategy} — по кодам ИЛИ по группе,
 * решается при сборке в App) и, если такие есть, шлёт в Telegram
 * сообщение = карточки релевантных товаров (тот же AlertFormatter, что baseline) + ссылка на первоисточник.
 * Если релевантных нет — молчит (сообщения, которого не должно быть, не создаём).
 */
public class DozorReporter {
    private final ProductLoader productLoader;
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceStrategy relevanceStrategy;
    private final AlertFormatter alertFormatter;
    private final Broadcaster broadcaster;

    public DozorReporter(ProductLoader productLoader, ArticleTextFetcher articleTextFetcher,
                         RelevanceStrategy relevanceStrategy, AlertFormatter alertFormatter,
                         Broadcaster broadcaster) {
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

        if (alertFormatter == null) {
            throw new IllegalArgumentException("alertFormatter не может быть null!");
        }
        this.alertFormatter = alertFormatter;

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
        for (Product p : relevant) {
            sb.append(alertFormatter.format(p)).append("\n\n");
        }
        sb.append("📄 <a href=\"").append(documentUrl).append("\">Первоисточник</a>\n\n");
        broadcaster.broadcast(sb.toString());
    }
}
