package org.regdozor;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * "Разборщик": превращает HTML-страницу поисковой выдачи pravo.gov.ru
 * в список структурированных объектов DocumentItem.
 * Использует библиотеку Jsoup и CSS-селекторы (как в вёрстке) для поиска нужных кусков.
 * Методы статические — состояния у парсера нет, он просто "функция": HTML на входе, список на выходе.
 */
public class PravoSearchParser {
    /**
     * Находит в документе все строки выдачи и парсит каждую.
     * Строки, которые не удалось распарсить, пропускаются (не роняют весь разбор).
     *
     * @param doc распарсенный Jsoup-документ одной страницы выдачи
     * @return список успешно разобранных документов (может быть пустым)
     */
    static List<DocumentItem> parse(Document doc) {
        // Находим все документы в выдаче
        Elements resultItems = doc.select("div.documents-table-row");
        List<DocumentItem> documentItems = new ArrayList<>();

        // Если ничего не найдено — дальше идти смысла нет
        if (resultItems.isEmpty()) {
            return documentItems;
        }

        for (int i = 0; i < resultItems.size(); i++) {
            Element item = resultItems.get(i);
            String label = "#" + (i + 1);

            try {
                DocumentItem documentItem = parseOneItem(item);

                label = "#" + (i + 1) +
                        " | eo = " + documentItem.eoNumber() +
                        " | publishDate = " + documentItem.publishDate();

                documentItems.add(documentItem);
                System.out.println("OK " + label);
            } catch (IllegalArgumentException e) {
                System.out.println("SKIP " + label + ": " + e.getMessage());
            }
        }
        return documentItems;
    }

    /**
     * Разбирает ОДНУ строку выдачи в DocumentItem.
     * Вытаскивает: ссылку и заголовок, eoNumber (из хвоста href), ссылку на PDF, дату опубликования.
     *
     * @param item элемент div.documents-table-row — одна строка выдачи
     * @return заполненный DocumentItem
     * @throws IllegalArgumentException если какого-то обязательного куска на странице не оказалось
     */
    static DocumentItem parseOneItem(Element item) {
        Element link;
        String href;

        // Ищем ссылку с названием документа
        link = item.selectFirst("a.documents-item-name");

        // Если ссылка не найдена — это уже другая ошибка, не "No rows found"
        if (link == null) {
            throw new IllegalArgumentException("Document link not found: a.documents-item-name");
        }

        // Достаём href (относительный) и текст ссылки (заголовок)
        href = link.attr("href");

        String title = link.text();
        String absUrl = link.absUrl("href");
        String eoNumber;

        // Вытаскиваем номер документа
        String hrefTrim = href.trim();


        if (hrefTrim.isEmpty()) {
            throw new IllegalArgumentException("Empty href");
        } else {
            int lastIndex = hrefTrim.lastIndexOf("/");
            if (lastIndex == -1) {
                throw new IllegalArgumentException("Unexpected href format (no '/')");
            } else {
                eoNumber = hrefTrim.substring(lastIndex + 1);
            }
        }

        // Вытаскиваем PDF
        Element file = item.selectFirst("a.documents-item-file");
        if (file == null) {
            throw new IllegalArgumentException("PDF link not found: a.documents-item-file");
        }

        String pdfAbsUrl = file.absUrl("href");

        // Дата публикации документа
        String publishDate = "";
        Element info = item.selectFirst("div.infoindocumentlist");
        if (info == null) {
            throw new IllegalArgumentException("info block not found");
        }

        Elements div = info.select("> div");
        if (div.isEmpty()) {
            throw new IllegalArgumentException("div block not found");
        }

        for (Element element : div) {
            String nameText = "";
            Element infoName = element.selectFirst("span.info-name");
            if (infoName != null) {
                nameText = infoName.text();
            }

            if (nameText.contains("Дата опубликования")) {
                Element infoData = element.selectFirst("span.info-data");
                if (infoData != null) {
                    publishDate = infoData.text();
                    break;
                }
            }
        }

        if (publishDate.isEmpty()) {
            throw new IllegalArgumentException("Publish date not found");
        }

        DocumentItem documentItem = new DocumentItem(eoNumber, publishDate, absUrl, pdfAbsUrl, title);

        return documentItem;
    }
}
