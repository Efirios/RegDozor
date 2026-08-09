package org.regdozor.operator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.regdozor.net.HttpTextFetcher;

/**
 * Второй кирпич "дозора": по URL отдаёт разобранную страницу и её ЧИСТЫЙ текст.
 * Делает то, что одинаково для любого сайта: качает HTML (HttpTextFetcher) и парсит его в Document (jsoup).
 * САЙТ-СПЕЦИФИЧНОЕ извлечение текста делегирует внедрённому ArticleExtractor — конкретную реализацию
 * (CrptReleaseExtractor: .text-par-lh-big; MarkirovkaReleaseExtractor: убрать [class*=comment], взять весь текст)
 * выбирают снаружи, при сборке в App. Так этот класс не знает, честныйзнак это или markirovka.
 *
 * ⚠️ ДВА ШАГА РАЗВЕДЕНЫ НАРОЧНО ({@link #fetchDocument} и {@link #extractText}). Одну и ту же страницу
 * читают ДВА потребителя: извлекатель текста (для матча по кодам) и {@link MarkirovkaStagesExtractor}
 * (для цитаты этапов). Скачав один раз и передав им общий {@link Document}, мы:
 * (1) не дёргаем чужой сайт дважды; (2) гарантируем, что матч и цитата — из ОДНОЙ версии страницы
 * (при двух запросах её могли бы отредактировать между ними). Это «поднять работу выше», а не кеш:
 * никакой скрытой памяти, значение живёт ровно пока обрабатывается статья.
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
     * Качает страницу и разбирает её в дерево. ЕДИНСТВЕННОЕ место, где страница скачивается.
     *
     * ⚠️ Второй аргумент {@code Jsoup.parse(html, url)} — БАЗОВЫЙ АДРЕС, и это не формальность:
     * относительные ссылки на странице ({@code href="/community/…"}) считаются относительно него.
     * Без базы {@code absUrl("href")} вернул бы ПУСТУЮ строку — молча. База = адрес страницы, которую
     * только что скачали, поэтому класс остаётся сайт-нейтральным: для честныйзнака придёт его адрес,
     * для markirovka — его.
     * ⚠️ Известное ограничение: качалка идёт по редиректам, но наружу отдаёт только тело — конечный
     * адрес неизвестен. При перенаправлении база будет чуть неверной.
     *
     * @param url адрес статьи
     * @return разобранная страница; её можно отдать нескольким извлекателям
     */
    public Document fetchDocument(String url) {
        String html = fetcher.fetch(url);   // общее: скачать (единственный сетевой запрос)
        return Jsoup.parse(html, url);      // общее: разобрать в дерево; url — базовый адрес для ссылок
    }

    /**
     * Достаёт из уже разобранной страницы чистый текст — сайт-специфично.
     *
     * Отдельным методом, потому что {@link ArticleExtractor} лежит в приватном поле: снаружи
     * ({@code DozorReporter}) до него иначе не добраться, а страницу он получает готовой.
     *
     * @param doc разобранная страница (из {@link #fetchDocument})
     * @return текст, готовый для матчинга
     */
    public String extractText(Document doc) {
        return extractor.extractText(doc);  // САЙТ-СПЕЦИФИЧНОЕ: делегируем внедрённой реализации
    }

    /**
     * По адресу статьи отдаёт её чистый текст (без меню, шапки и прочего шума).
     *
     * @param url адрес статьи
     * @return текст, готовый для матчинга (по кодам или по названию группы)
     */
    public String fetchCleanText(String url) {
        return extractText(fetchDocument(url));
    }
}
