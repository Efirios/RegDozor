package org.regdozor;

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
    static List<String> parse(Document doc) {
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
