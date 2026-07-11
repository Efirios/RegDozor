package org.regdozor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Set;

/**
 * Монитор ленты релизов ЦРПТ: скачивает честныйзнак.рф/info/releasenotes/,
 * парсит ссылки на выпуски (CrptReleaseNotesParser), через SeenStore("seen-releases.txt")
 * отбирает НОВЫЕ (не виденные) и по каждому зовёт DozorReporter.run(url).
 * Порядок: seen.add() ПОСЛЕ dozorReporter.run() (доставка "хотя бы один раз"); save() в конце.
 */
public class CrptFeedMonitor {
    private final HttpTextFetcher fetcher;
    private final SeenStore seenStore;
    private final DozorReporter dozorReporter;

    public CrptFeedMonitor(HttpTextFetcher fetcher, SeenStore seenStore, DozorReporter dozorReporter) {
        if (fetcher == null) {
            throw new IllegalArgumentException("fetcher не может быть null!");
        }
        this.fetcher = fetcher;

        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (dozorReporter == null) {
            throw new IllegalArgumentException("dozorReporter не может быть null!");
        }
        this.dozorReporter = dozorReporter;
    }

    public void run() {
        final String feedUrl = "https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/";
        String html = fetcher.fetch(feedUrl);
        Document doc = Jsoup.parse(html, "https://xn--80ajghhoc2aj1c8b.xn--p1ai");
        List<String> releases = CrptReleaseNotesParser.parse(doc);
        Set<String> seen = seenStore.load();

        for (String url : releases) {
            if (!seen.contains(url)) {
                dozorReporter.run(url);
                seen.add(url);
            }
        }

        seenStore.save(seen);
    }
}
