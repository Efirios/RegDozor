package org.bizassistant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

public class App {
    public static void main(String[] args) {

        // Читаем HTML из resources
        String html = ResourceTextReader.read();

        // Парсим HTML в Document, baseUri нужен для absUrl(...)
        Document doc = Jsoup.parse(html, "https://publication.pravo.gov.ru");

        // Обрабатываем все элементы выдачи
        List<DocumentItem> items = PravoSearchParser.parse(doc);

        System.out.println("parsed = " + items.size());

        if (!items.isEmpty()) {
            System.out.println(items.get(0));
        }
    }
}