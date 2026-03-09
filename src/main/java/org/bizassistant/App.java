package org.bizassistant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.util.List;

public class App {
    public static void main(String[] args) {

        String url = "http://publication.pravo.gov.ru/search?&pageSize=30&index=1&SignatoryAuthorityId=852b803d-" +
                "4d5b-4ed1-aaca-03284c0bc35e&DocumentTypes=2dddb344-d3e2-4785-a899-7aa12bd47b6f&PublishDateSearchType=" +
                "0&NumberSearchType=0&DocumentDateSearchType=0&JdRegSearchType=0&Name=средствами%20идентификации&" +
                "SortedBy=6&SortDestination=1";

        HttpTextFetcher httpTextFetcher = new HttpTextFetcher();
        String html = httpTextFetcher.fetch(url);

        System.out.println(html.length());

        Document doc = Jsoup.parse(html, "http://publication.pravo.gov.ru");

        List<DocumentItem> items = PravoSearchParser.parse(doc);

        System.out.println("parsed = " + items.size());

        if (!items.isEmpty()) {
            System.out.println(items.get(0));
        }
    }
}