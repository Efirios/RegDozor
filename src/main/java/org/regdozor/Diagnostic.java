package org.regdozor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * ВРЕМЕННЫЙ отладочный класс (не часть продукта, удалить при выносе движка "дозора" в боевой код).
 * Прогоняет ядро "дозора" на двух реальных страницах ЦРПТ: fetch чистого текста -> RelevanceChecker.
 * Ожидание: волна-3 (mart-2025) -> [майка], волна-4 (s-1-marta-2026) -> [] (ложный флаг 6104 отсечён ОКПД2).
 */
public class Diagnostic {
    public static void main(String[] args) {
//        HttpTextFetcher fetcher = new HttpTextFetcher();
//        CodeMatcher matcher = new CodeMatcher();
//        RelevanceChecker relevanceChecker = new RelevanceChecker(matcher);
//        ArticleTextFetcher articleTextFetcher = new ArticleTextFetcher(fetcher);
//
//        String url1 = "https://markirovka.ru/community/shoes-and-clothes/rasshirenie-perechnya-tovarov-legkoy-" +
//                "promyshlennosti-mart-2025";
//
//
//        String url2 = "https://markirovka.ru/community/shoes-and-clothes/rasshirenie-perechnya-tovarov-legkoy-" +
//                "promyshlennosti-s-1-marta-2026";
//
//        Product[] products = new ProductLoader().load();
//
//        System.out.println(relevanceChecker.findRelevant(articleTextFetcher.fetchCleanText(url1), products));
//        System.out.println(relevanceChecker.findRelevant(articleTextFetcher.fetchCleanText(url2), products));

//        String feed = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/";
//        String html = new HttpTextFetcher().fetch(feed);
//        Document doc = Jsoup.parse(html, "https://xn--80ajghhoc2aj1c8b.xn--p1ai");
//        List<String> releases = CrptReleaseNotesParser.parse(doc);
//        releases.forEach(System.out::println);
//        System.out.println("Всего выпусков: " + releases.size());


//        System.out.println("Длина: " + html.length());
//        System.out.println("Есть ссылки на выпуски: " + html.contains("chto-novogo-v-sisteme"));
//        try {
//            Files.writeString(Path.of("data", "feed.html"), html);   // сохрани, гляну структуру <a>
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        String text = new ArticleTextFetcher(new HttpTextFetcher()).fetchCleanText("https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-22-06-2026-po-26-06-2026/");
//        String html = new HttpTextFetcher().fetch("https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-22-06-2026-po-26-06-2026/");
//        try {
//            Files.writeString(Path.of("data", "release.html"), html);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        Profile profile = new ProfileLoader().load();
//        System.out.println(new GroupMatcher().concernsGroup(text,profile));

        String url1 = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-26-01-2026-po-30-01-2026/";
        String url2 = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-27-04-2026-po-30-04-2026/";
        String url3 = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-12-01-2026-po-16-01-2026/";
        String url4 = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-22-06-2026-po-26-06-2026/";

        List<String> urls = List.of(url1, url2, url3, url4);
        ArticleTextFetcher fetcher = new ArticleTextFetcher(new HttpTextFetcher(), new CrptReleaseExtractor());
        Profile profile = new ProfileLoader().load();
        GroupMatcher matcher = new GroupMatcher();
        for (String url : urls) {
            boolean relevant = matcher.concernsGroup(fetcher.fetchCleanText(url), profile);
            System.out.println(relevant + " ← " + url);
        }

    }
}
