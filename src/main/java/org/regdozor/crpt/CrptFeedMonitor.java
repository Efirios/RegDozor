package org.regdozor.crpt;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.regdozor.report.DozorReporter;
import org.regdozor.net.HttpTextFetcher;
import org.regdozor.store.SeenStore;

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

    /**
     * Один прогон дозора: скачать ленту релизов, взять ссылки на выпуски, отобрать НОВЫЕ (по SeenStore)
     * и прогнать каждый через DozorReporter. Зовётся планировщиком раз в сутки.
     * Порядок: seen.add() ПОСЛЕ dozorReporter.run(), save() в самом конце — доставка «хотя бы один раз»
     * (если отправка упадёт, ссылка не запомнится и на следующем прогоне попробуем снова).
     */
    public void run() {
        // Домен кириллический (честныйзнак.рф), но HttpTextFetcher не умеет переводить ХОСТ в punycode —
        // поэтому пишем адрес сразу в punycode-виде. TODO: научить fetcher java.net.IDN.toASCII(host).
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
