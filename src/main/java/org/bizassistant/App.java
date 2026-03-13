package org.bizassistant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Subscription minPromIdentification = new Subscription(
                "MinPromIdentification",
                "http://publication.pravo.gov.ru/search?&pageSize=30&index=1&SignatoryAuthorityId=" +
                        "852b803d-4d5b-4ed1-aaca-03284c0bc35e&DocumentTypes=2dddb344-d3e2-4785-a899-" +
                        "7aa12bd47b6f&PublishDateSearchType=0&NumberSearchType=0&DocumentDateSearchType=" +
                        "0&JdRegSearchType=0&Name=средствами%20идентификации&SortedBy=6&SortDestination=1",
                1);

        Subscription govIdentification = new Subscription(
                "GovIdentification",
                "http://publication.pravo.gov.ru/search?&pageSize=30&index=1&SignatoryAuthorityId=" +
                        "8005d8c9-4b6d-48d3-861a-2a37e69fccb3&DocumentTypes=fd5a8766-f6fd-4ac2-8fd9-66f414d314ac&" +
                        "PublishDateSearchType=0&NumberSearchType=0&DocumentDateSearchType=0&JdRegSearchType=" +
                        "0&Name=средствами%20идентификации&SortedBy=6&SortDestination=1",
                2);

        Subscription govMark = new Subscription(
                "GovMark",
                "http://publication.pravo.gov.ru/search?&pageSize=30&index=1&SignatoryAuthorityId=" +
                        "8005d8c9-4b6d-48d3-861a-2a37e69fccb3&DocumentTypes=fd5a8766-f6fd-4ac2-8fd9-" +
                        "66f414d314ac&PublishDateSearchType=0&NumberSearchType=0&DocumentDateSearchType=" +
                        "0&JdRegSearchType=0&Name=маркировки&SortedBy=6&SortDestination=1",
                2);
        List<Subscription> subscriptions = List.of(minPromIdentification, govIdentification, govMark);
        HttpTextFetcher httpTextFetcher = new HttpTextFetcher();
        MonitorRunner runner = new MonitorRunner(subscriptions, httpTextFetcher);
        runner.run();





//        String url = "http://publication.pravo.gov.ru/search?&pageSize=30&index=1&SignatoryAuthorityId=852b803d-" +
//                "4d5b-4ed1-aaca-03284c0bc35e&DocumentTypes=2dddb344-d3e2-4785-a899-7aa12bd47b6f&PublishDateSearchType=" +
//                "0&NumberSearchType=0&DocumentDateSearchType=0&JdRegSearchType=0&Name=средствами%20идентификации&" +
//                "SortedBy=6&SortDestination=1";
//
//        HttpTextFetcher httpTextFetcher = new HttpTextFetcher();
//        String html = httpTextFetcher.fetch(url);
//
//        System.out.println(html.length());
//
    }
}