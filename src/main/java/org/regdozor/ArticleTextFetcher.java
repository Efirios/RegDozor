package org.regdozor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Второй кирпич "дозора": по URL отдаёт ЧИСТЫЙ текст статьи.
 * ВНИМАНИЕ (временное состояние): извлечение СЕЙЧАС заточено под честныйзнак.рф (берёт .text-par-lh-big —
 * только тело статьи, иначе меню/хештеги дают ложные совпадения). Строка про markirovka
 * ([class*=comment].remove()) ЗАКОММЕНТИРОВАНА → для markirovka класс сейчас НЕ работает.
 * TODO: развязать через интерфейс ArticleExtractor (реализация на каждый сайт: markirovka убирает
 * комментарии, честныйзнак берёт .text-par-lh-big). Извлечение текста сайт-специфично.
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
//        doc.select("[class*=comment]").remove();
//        return doc.text();
        return doc.select(".text-par-lh-big").text();
    }
}
