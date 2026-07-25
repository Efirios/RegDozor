package org.regdozor.pravo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.regdozor.net.HttpTextFetcher;
import org.regdozor.store.SeenStore;
import org.regdozor.telegram.Broadcaster;

import java.util.*;

/**
 * "Дирижёр": связывает всех помощников в единый процесс мониторинга.
 * Для каждой подписки листает страницы, скачивает (fetcher), парсит (PravoSearchParser),
 * убирает дубли внутри прогона и сравнивает с памятью (seenStore),
 * чтобы выделить НОВЫЕ документы, о которых оповещает в Telegram (telegramNotifier).
 */
public class MonitorRunner {
    /** Список того, что мониторим. Задаётся снаружи через конструктор. */
    private final List<Subscription> subscriptions;
    /** Помощник "скачать HTML". */
    private final HttpTextFetcher fetcher;
    /** Помощник "память": что мы уже видели в прошлых запусках. */
    private final SeenStore seenStore;
    /** «Рассыльщик»: шлёт сообщения всем подписчикам из реестра. */
    private final Broadcaster broadcaster;

    /**
     * Все зависимости приходят снаружи (dependency injection) и проверяются на null.
     * Так MonitorRunner не привязан к конкретным реализациям и его легко тестировать.
     */
    public MonitorRunner(List<Subscription> subscriptions, HttpTextFetcher fetcher, SeenStore seenStore,
                         Broadcaster broadcaster) {
        if (subscriptions == null || subscriptions.isEmpty()){
            throw new IllegalArgumentException("subscriptions не может быть null или пустым!");
        }
        this.subscriptions = subscriptions;

        if (fetcher == null){
            throw new IllegalArgumentException("fetcher не может быть null!");
        }
        this.fetcher = fetcher;

        if (seenStore == null){
            throw new IllegalArgumentException("seenStore не может быть null!");
        }
        this.seenStore = seenStore;

        if (broadcaster == null){
            throw new IllegalArgumentException("broadcaster не может быть null!");
        }
        this.broadcaster = broadcaster;
    }

    /**
     * Главный метод: прогоняет мониторинг по всем подпискам.
     * Собирает все уникальные документы прогона, сравнивает их с памятью (seen),
     * выделяет новинки и отправляет их в Telegram, затем сохраняет обновлённую память.
     *
     * ВАЖЕН ПОРЯДОК: отправка идёт ДО save(). Если отправка упадёт, память не сохранится,
     * и на следующем запуске документы снова будут "новыми" -> уведомление повторится.
     * Лучше продублировать уведомление, чем потерять его (доставка "хотя бы один раз").
     */
    public void run() {
        System.out.println("Количество подписок = " + subscriptions.size());
        String name;
        int maxPages;

        // Карта для устранения дублей ВНУТРИ одного прогона:
        // один и тот же документ может встретиться на разных страницах/подписках.
        // Ключ — eoNumber, значение — сам документ.
        Map<String, DocumentItem> unique = new HashMap<>();
        Set<String> seen = seenStore.load();

        // Внешний цикл: по каждой подписке.
        for (Subscription sub : subscriptions) {
            name = sub.name();
            maxPages = sub.maxPages();

            System.out.println("Название подписки: " + name);
            System.out.println("Количество страниц для парсинга: " + maxPages);

            // Внутренний цикл: по страницам выдачи этой подписки (пагинация).
            for (int page = 1; page <= maxPages; page++) {
                // Подставляем номер нужной страницы в URL (index=1 -> index=N).
                String pageUrl = buildPageUrl(sub.baseUrl(), page);

                try {
                    // 1) Скачать HTML страницы.
                    String html = fetcher.fetch(pageUrl);

                    // 2) Превратить текст в дерево Jsoup (второй аргумент — базовый URL для относительных ссылок).
                    Document doc = Jsoup.parse(html, "http://publication.pravo.gov.ru");

                    // 3) Разобрать дерево в список документов.
                    List<DocumentItem> items = PravoSearchParser.parse(doc);

                    int addedThisPage = 0;

                    // 4) Складываем в общую карту только те, которых там ещё нет.
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
                    // Ошибка на одной странице не должна ронять весь мониторинг —
                    // логируем и идём дальше к следующей странице.
                    System.out.println("FAIL " + name + " page=" + page + " url=" + pageUrl + ": " + e.getMessage());
                }

                try {
                    Thread.sleep(1000);
                }  catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        List<DocumentItem> newItems = new ArrayList<>();
        for (DocumentItem di : unique.values()) {
            if (!seen.contains(di.eoNumber())) {
                newItems.add(di);
                seen.add(di.eoNumber());
            }
        }

        for (DocumentItem di : newItems) {
            broadcaster.broadcast(di.title() + "\n" + di.documentUrl());
        }

        seenStore.save(seen);

        System.out.println("=== НОВЫЕ документы: " + newItems.size() + " ===");

        for (DocumentItem di : newItems) {
            System.out.println(di);
        }

        System.out.println("TOTAL UNIQUE = " + unique.size());
    }

    /**
     * Заменяет в URL номер страницы: параметр index=<любое число> -> index=page.
     *
     * @param baseUrl исходный URL (с index=1)
     * @param page    нужный номер страницы
     * @return URL, указывающий на нужную страницу выдачи
     */
    private String buildPageUrl(String baseUrl, int page) {
        return baseUrl.replaceFirst("index=\\d+", "index=" + page);
    }
}
