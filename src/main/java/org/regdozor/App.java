package org.regdozor;

import java.util.List;

/**
 * Точка входа в приложение (класс с методом main).
 * Здесь мы "собираем" программу: читаем секреты, описываем подписки, создаём помощников
 * и двух координаторов — BaselineReporter (рассылка каталога обязанностей)
 * и MonitorRunner (мониторинг pravo.gov.ru) — и запускаем оба.
 * Вся реальная логика — в других классах; App только связывает их вместе.
 */
public class App {
    public static void main(String[] args) {
        String token = System.getenv("TG_BOT_TOKEN");
        if (token == null || token.isBlank()){
            throw new IllegalStateException("не задана переменная окружения TG_BOT_TOKEN");
        }

        String chatId = System.getenv("TG_CHAT_ID");
        if (chatId == null || chatId.isBlank()){
            throw new IllegalStateException("не задана переменная окружения TG_CHAT_ID");
        }

        // Печатаем рабочую директорию — чтобы понимать, откуда программа ищет data/seen-eonumbers.txt.
        System.out.println(System.getProperty("user.dir"));

        // ВНИМАНИЕ: URL-ы ниже намеренно на http, а НЕ https.
        // pravo.gov.ru обрывает TLS-рукопожатие для обычного Java-клиента
        // (SSLHandshakeException: Remote host terminated the handshake) —
        // вероятно, из-за ГОСТ-шифрования или защиты от ботов. Стандартная Java
        // такое TLS не умеет без отдельного крипто-провайдера. По http всё работает.
        // Не менять на https, пока не решён вопрос с ГОСТ-TLS (задача на будущее).

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

        // Собираем все подписки в один неизменяемый список.
        List<Subscription> subscriptions = List.of(minPromIdentification, govIdentification, govMark);

        // Создаём помощников СНАРУЖИ и передаём внутрь runner (dependency injection).
        HttpTextFetcher httpTextFetcher = new HttpTextFetcher();   // "качалка" HTML
        SeenEoNumberStore seenStore = new SeenEoNumberStore();     // "память" о виденном
        TelegramNotifier telegramNotifier = new TelegramNotifier(token, chatId);
        ProductLoader productLoader = new ProductLoader();
        AlertFormatter alertFormatter = new AlertFormatter();
        BaselineReporter baselineReporter = new BaselineReporter(productLoader, alertFormatter, telegramNotifier);

        // Дирижёр получает подписки и всех помощников — и запускает процесс.
        MonitorRunner runner = new MonitorRunner(subscriptions, httpTextFetcher, seenStore, telegramNotifier);

        baselineReporter.run();
        runner.run();
    }
}