package org.regdozor.operator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.regdozor.net.HttpTextFetcher;

/**
 * Второй кирпич "дозора": по URL отдаёт ЧИСТЫЙ текст статьи.
 * Делает то, что одинаково для любого сайта: качает HTML (HttpTextFetcher) и парсит его в Document (jsoup).
 * САЙТ-СПЕЦИФИЧНОЕ извлечение текста делегирует внедрённому ArticleExtractor — конкретную реализацию
 * (CrptReleaseExtractor: .text-par-lh-big; MarkirovkaReleaseExtractor: убрать [class*=comment], взять весь текст)
 * выбирают снаружи, при сборке в App. Так этот класс не знает, честныйзнак это или markirovka.
 */
public class ArticleTextFetcher {
    private final HttpTextFetcher fetcher;
    private final ArticleExtractor extractor;

    public ArticleTextFetcher(HttpTextFetcher fetcher, ArticleExtractor extractor) {
        if (fetcher == null) {
            throw new IllegalArgumentException("fetcher не может быть null!");
        }
        this.fetcher = fetcher;

        if (extractor == null) {
            throw new IllegalArgumentException("extractor не может быть null!");
        }
        this.extractor = extractor;
    }

    /**
     * По адресу статьи отдаёт её чистый текст (без меню, шапки и прочего шума).
     *
     * @param url адрес статьи
     * @return текст, готовый для матчинга (по кодам или по названию группы)
     */
    public String fetchCleanText(String url) {
        String html = fetcher.fetch(url);      // общее: скачать
        Document doc = Jsoup.parse(html);      // общее: разобрать HTML в дерево
        return extractor.extractText(doc);     // САЙТ-СПЕЦИФИЧНОЕ: делегируем внедрённой реализации
    }
}
