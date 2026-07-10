package org.regdozor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Второй кирпич "дозора": по URL отдаёт ЧИСТЫЙ текст статьи.
 * Качает (fetcher) -> парсит jsoup -> ВЫРЕЗАЕТ комментарии ([class*=comment], иначе вопросы юзеров
 * дают ложные совпадения кодов) -> возвращает видимый текст.
 * ВНИМАНИЕ: селектор "[class*=comment]" — специфика markirovka.ru; для другого источника чистка будет иной.
 */
public class ArticleTextFetcher {
    private final HttpTextFetcher fetcher;

    public ArticleTextFetcher(HttpTextFetcher fetcher) {
        if (fetcher == null) {
            throw new IllegalArgumentException("fetcher не может быть null!");
        }
        this.fetcher = fetcher;
    }

    public String fetchCleanText(String url) {
        String html = fetcher.fetch(url);
        Document doc = Jsoup.parse(html);
        doc.select("[class*=comment]").remove();
        return doc.text();
    }
}
