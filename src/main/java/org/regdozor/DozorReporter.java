package org.regdozor;

import java.util.List;

/**
 * Координатор "дозора" для ОДНОГО документа: run(url) скачивает чистый текст,
 * находит релевантные товары (по ТН ВЭД+ОКПД2) и, если такие есть, шлёт в Telegram
 * сообщение = ссылка на первоисточник + карточки релевантных товаров (тот же AlertFormatter, что baseline).
 * Если релевантных нет — молчит (сообщения, которого не должно быть, не создаём).
 */
public class DozorReporter {
    private final ProductLoader productLoader;
    private final ArticleTextFetcher articleTextFetcher;
    private final RelevanceChecker relevanceChecker;
    private final AlertFormatter alertFormatter;
    private final TelegramNotifier telegramNotifier;

    public DozorReporter(ProductLoader productLoader, ArticleTextFetcher articleTextFetcher,
                         RelevanceChecker relevanceChecker, AlertFormatter alertFormatter,
                         TelegramNotifier telegramNotifier) {
        if (productLoader == null) {
            throw new IllegalArgumentException("productLoader не может быть null!");
        }
        this.productLoader = productLoader;

        if (articleTextFetcher == null) {
            throw new IllegalArgumentException("articleTextFetcher не может быть null!");
        }
        this.articleTextFetcher = articleTextFetcher;

        if (relevanceChecker == null) {
            throw new IllegalArgumentException("relevanceChecker не может быть null!");
        }
        this.relevanceChecker = relevanceChecker;

        if (alertFormatter == null) {
            throw new IllegalArgumentException("alertFormatter не может быть null!");
        }
        this.alertFormatter = alertFormatter;

        if (telegramNotifier == null) {
            throw new IllegalArgumentException("telegramNotifier не может быть null!");
        }
        this.telegramNotifier = telegramNotifier;
    }

    public void run(String documentUrl) {
        Product[] products = productLoader.load();
        String text = articleTextFetcher.fetchCleanText(documentUrl);
        List<Product> relevant = relevanceChecker.findRelevant(text, products);

        if (relevant.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (Product p : relevant) {
            sb.append(alertFormatter.format(p)).append("\n\n");
        }
        sb.append("📄 <a href=\"").append(documentUrl).append("\">Первоисточник</a>\n\n");
        telegramNotifier.send(sb.toString());
    }
}
