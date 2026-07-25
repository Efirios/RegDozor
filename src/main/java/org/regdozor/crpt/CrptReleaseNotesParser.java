package org.regdozor.crpt;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * "Разборщик ленты релизов ЦРПТ": из страницы честныйзнак.рф/info/releasenotes/
 * достаёт список ссылок на выпуски «Что нового в системе».
 * Селектор a.news-list3__title — только заголовки-ссылки (без дублей-кнопок more4).
 * Возвращает АБСОЛЮТНЫЕ URL (absUrl работает, т.к. документ парсится с базовым URL сайта).
 * Схема имени как у PravoSearchParser: &lt;Источник&gt;&lt;Что&gt;Parser.
 */
public class CrptReleaseNotesParser {
    /**
     * Достаёт из страницы ленты ссылки на все выпуски.
     *
     * @param doc распарсенная страница ленты. ВАЖНО: её надо парсить с БАЗОВЫМ URL сайта
     *            (Jsoup.parse(html, baseUrl)) — иначе absUrl() не сможет достроить относительные ссылки.
     * @return абсолютные ссылки на выпуски (пустой список, если ничего не нашлось)
     */
    static List<String> parse(Document doc) {
        // Селектор берёт только заголовки-ссылки выпусков — без дублей от кнопок «читать далее».
        Elements links = doc.select("a.news-list3__title");
        List<String> list = new ArrayList<>();

        if (links.isEmpty()) {
            return list;
        }

        for (Element link : links) {
           list.add(link.absUrl("href"));
        }

        return list;
    }
}
