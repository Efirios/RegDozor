package org.regdozor.operator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Разборщик ленты «Сообщество» markirovka.ru: достаёт ссылки на все статьи.
 *
 * Единственное знание класса про markirovka — селектор a.publication-card__list-link
 * (карточки статей в списке). Схема имени как у {@link CrptReleaseNotesParser}: &lt;Источник&gt;&lt;Что&gt;Parser.
 *
 * Список НЕ фильтруется — здесь ВСЕ статьи всех разделов (269 на момент разведки). Отбор по
 * разделу пользователя (/community/&lt;раздел&gt;/) — забота монитора, не парсера: так класс остаётся
 * «под всех» и не знает ни про какой конкретный товар.
 */
public class MarkirovkaCommunityParser {
    /**
     * Достаёт из страницы ленты абсолютные ссылки на все статьи.
     *
     * @param doc распарсенная страница /community/. ⚠️ Её надо парсить С БАЗОВЫМ URL сайта
     *            (Jsoup.parse(html, "https://markirovka.ru")) — иначе absUrl("href") вернёт
     *            не абсолютную ссылку, а ПУСТУЮ строку (тихо, без ошибки), и дозор промолчит.
     * @return абсолютные ссылки на статьи (пустой список, если ничего не нашлось)
     */
    static List<String> parse(Document doc) {
        Elements links = doc.select("a.publication-card__list-link");
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
