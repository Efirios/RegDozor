package org.regdozor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * За один прогон ловит новые статьи ЛЕНТЫ markirovka в разделе пользователя и по каждой зовёт дозор.
 *
 * Брат-близнец {@link CrptFeedMonitor}: та же роль «дирижёра» (скачать ленту → разобрать →
 * отобрать новое → раздать дозору), отличается тремя вещами — источник (markirovka.ru/community/,
 * а не честныйзнак), разборщик ({@link MarkirovkaCommunityParser}) и НОВЫЙ шаг: фильтр по разделу.
 *
 * Фильтр по разделу — почему он тут. Лента /community/ смешивает ВСЕ разделы (269 статей на момент
 * разведки), а пользователю нужен только его (у Лии — «shoes-and-clothes», 16 статей). Раздел лежит
 * в URL между /community/ и следующим «/» и берётся ИЗ ПРОФИЛЯ ({@link Profile#section()}), а не из
 * зашитого литерала — так класс работает «под всех», а не под одну Лию.
 *
 * Сам ничего не парсит и не матчит — только связывает помощников и держит порядок доставки.
 */
public class MarkirovkaFeedMonitor {
    private final HttpTextFetcher httpTextFetcher;
    private final SeenStore seenStore;
    private final DozorReporter dozorReporter;
    private final Profile profile;

    public MarkirovkaFeedMonitor(HttpTextFetcher httpTextFetcher, SeenStore seenStore, DozorReporter dozorReporter,
                                 Profile profile) {
        if (httpTextFetcher == null) {
            throw new IllegalArgumentException("httpTextFetcher не может быть null!");
        }
        this.httpTextFetcher = httpTextFetcher;

        if (seenStore == null) {
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (dozorReporter == null) {
            throw new IllegalArgumentException("dozorReporter не может быть null!");
        }
        this.dozorReporter = dozorReporter;

        if (profile == null) {
            throw new IllegalArgumentException("profile не может быть null!");
        }
        this.profile = profile;
    }

    /**
     * Один прогон дозора markirovka. Зовётся планировщиком.
     *
     * Поток: скачать ленту → разобрать в дерево → достать все ссылки → ОСТАВИТЬ только раздел(ы)
     * пользователя → отобрать невиданные (SeenStore) → по каждой новой позвать DozorReporter → сохранить.
     *
     * ⚠️ Jsoup.parse вызывается С БАЗОВЫМ адресом ("https://markirovka.ru") — без него absUrl("href")
     *    в разборщике тихо вернул бы пустые строки, и дозор молча промолчал бы (бесшумный отказ).
     * ⚠️ Порядок «хотя бы один раз»: seen.add() ПОСЛЕ dozorReporter.run(), save() в самом конце.
     *    Если отправка упадёт — ссылка не запомнится и на следующем прогоне попробуем снова. Не переставлять.
     * ⚠️ ХОЛОДНЫЙ СТАРТ ПРИРУЧЁН (проверено двумя прогонами подряд: 1-й молчит и заполняет, 2-й молчит).
     *    Пустой seen = памяти нет = «мы эту ленту ещё не видели». Тогда существующие статьи — НЕ новости,
     *    а фон: молча кладём их ВСЕ в seen и выходим, дозор не зовём. Иначе первый же прогон отправил бы
     *    архив раздела как алерты РЕАЛЬНОМУ клиенту (Лиа — живой подписчик).
     *    Заполняем ВСЕМИ ссылками ленты (links), а не только отфильтрованными (linkOk): это «снимок всего
     *    мира как известного» — тогда и при будущем расширении разделов в профиле старьё оттуда не всплывёт.
     *    Край: если лента вернула ПУСТО (сайт лёг / селектор умер), seen останется пустым и следующий прогон
     *    снова сочтёт себя холодным стартом — это защищает от «застолбить пустоту как истину».
     *
     * ⚠️ Это НЕ «холодный старт подписчика». Тут решается «не спамить архивом ТЕХ, КТО УЖЕ подписан».
     *    А «что действует по товарам НОВОГО человека при подписке» — отдельный механизм (сейчас это делает
     *    онбординг через baseline; после сноса baseline его должен заменить «вывод дозора при подписке»).
     */
    public void run(){
        List<String> linkOk = new ArrayList<>();
        final String html = httpTextFetcher.fetch("https://markirovka.ru/community/");
        Document doc = Jsoup.parse(html, "https://markirovka.ru");
        List<String> links = MarkirovkaCommunityParser.parse(doc);

        if (profile.section() == null) {
            throw new IllegalStateException("Профиль пользователя не содержит раздел");
        }

        for (String link : links) {
            for (String section : profile.section()) {
                if (link.contains("/" + section + "/")) {
                    linkOk.add(link);
                }
            }
        }

        Set<String> seen = seenStore.load();

        if (seen.isEmpty()) {
            seen.addAll(links);
            seenStore.save(seen);
            return;
        }

        for (String url : linkOk) {
            if (!seen.contains(url)) {
                dozorReporter.run(url);
                seen.add(url);
            }
        }

        seenStore.save(seen);
    }
}
