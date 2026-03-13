package org.bizassistant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitorRunner {
    private final List<Subscription> subscriptions;
    private final HttpTextFetcher fetcher;

    public MonitorRunner(List<Subscription> subscriptions, HttpTextFetcher fetcher) {
        if (subscriptions == null || subscriptions.isEmpty()){
            throw new IllegalArgumentException("subscriptions не может быть null или пустым!");
        }
        this.subscriptions = subscriptions;

        if (fetcher == null){
            throw new IllegalArgumentException("fetcher не может быть null!");
        }
        this.fetcher = fetcher;
    }

    public void run() {
        System.out.println("Количество подписок = " + subscriptions.size());
        String name;
        int maxPages;

        Map<String, DocumentItem> unique = new HashMap<>();

        for (Subscription sub : subscriptions) {
            name = sub.name();
            maxPages = sub.maxPages();

            System.out.println("Название подписки: " + name);
            System.out.println("Количество страниц для парсинга: " + maxPages);

            for (int page = 1; page <= maxPages; page++) {
                String pageUrl = buildPageUrl(sub.baseUrl(), page);

                try {
                    String html = fetcher.fetch(pageUrl);

                    Document doc = Jsoup.parse(html, "http://publication.pravo.gov.ru");

                    List<DocumentItem> items = PravoSearchParser.parse(doc);

                    int addedThisPage = 0;

                    for (DocumentItem di : items) {
                        if (!unique.containsKey(di.eoNumber())) {
                            unique.put(di.eoNumber(), di);
                            addedThisPage++;
                        }
                    }

                    System.out.println(name + " | page=" + page + " | parsed=" + items.size() + " | added=" +
                            addedThisPage + " | uniqueTotal=" + unique.size());

                    if (!items.isEmpty()) {
                        System.out.println(items.get(0));
                    }
                } catch (RuntimeException e) {
                    System.out.println("FAIL " + name + " page=" + page + " url=" + pageUrl + ": " + e.getMessage());
                }
            }
        }

        System.out.println("TOTAL UNIQUE = " + unique.size());
    }

    private String buildPageUrl(String baseUrl, int page) {
        return baseUrl.replaceFirst("index=\\d+", "index=" + page);
    }
}
