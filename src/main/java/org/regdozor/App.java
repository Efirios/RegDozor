package org.regdozor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.regdozor.catalog.AlertFormatter;
import org.regdozor.catalog.ProductLoader;
import org.regdozor.crpt.*;
import org.regdozor.match.*;
import org.regdozor.net.HttpTextFetcher;
import org.regdozor.pravo.MonitorRunner;
import org.regdozor.pravo.Subscription;
import org.regdozor.profile.Profile;
import org.regdozor.profile.ProfileLoader;
import org.regdozor.report.BaselineReporter;
import org.regdozor.report.DozorReporter;
import org.regdozor.store.OffsetStore;
import org.regdozor.store.SeenStore;
import org.regdozor.telegram.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Точка входа и место сборки всей программы.
 *
 * Класс с методом main и «composition root»: читаем секреты из окружения, описываем подписки,
 * создаём всех помощников и координаторов и внедряем их друг в друга (dependency injection). Именно здесь делается конкретный выбор
 * реализаций (напр. какой ArticleExtractor / RelevanceStrategy подставить).
 *
 * ТЕКУЩЕЕ RUN-СОСТОЯНИЕ: на планировщике крутятся ДВЕ задачи — дозор ЦРПТ (раз в сутки)
 * и приёмник подписчиков (раз в 2 сек, long polling). Мониторинг pravo (runner.run()) ЗАКОММЕНТИРОВАН.
 * Baseline-карточки БОЛЬШЕ НЕ шлются при старте: теперь это событие ПОДПИСКИ — их отправляет
 * OnboardingReporter новичку в момент, когда тот впервые написал боту.
 * Вся реальная логика — в других классах; App только связывает их вместе.
 */
public class App {
    public static void main(String[] args) {
        String token = System.getenv("TG_BOT_TOKEN");
        if (token == null || token.isBlank()){
            throw new IllegalStateException("не задана переменная окружения TG_BOT_TOKEN");
        }

//        String chatId = System.getenv("TG_CHAT_ID");
//        if (chatId == null || chatId.isBlank()){
//            throw new IllegalStateException("не задана переменная окружения TG_CHAT_ID");
//        }

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
        SeenStore seenStore = new SeenStore("seen-eonumbers.txt");     // "память" о виденном
        TelegramNotifier telegramNotifier = new TelegramNotifier(token);
        SeenStore subscriberStore = new SeenStore("chat-ids.txt");
        Broadcaster broadcaster = new Broadcaster(subscriberStore, telegramNotifier);
        ProductLoader productLoader = new ProductLoader();
        AlertFormatter alertFormatter = new AlertFormatter();
        BaselineReporter baselineReporter = new BaselineReporter(productLoader, alertFormatter, telegramNotifier);
        OnboardingReporter onboardingReporter = new OnboardingReporter(telegramNotifier, baselineReporter);
        ArticleExtractor articleExtractor = new CrptReleaseExtractor();
        ArticleExtractor markirovkaArticleExtractor = new MarkirovkaReleaseExtractor();

        ArticleTextFetcher articleTextFetcher = new ArticleTextFetcher(httpTextFetcher, articleExtractor);
        ArticleTextFetcher markirovkaArticleTextFetcher = new ArticleTextFetcher(httpTextFetcher, markirovkaArticleExtractor);
        GroupMatcher groupMatcher = new GroupMatcher();
        Profile profile = new ProfileLoader().load();
        RelevanceStrategy relevanceStrategy = new GroupRelevanceStrategy(groupMatcher, profile);
        RelevanceStrategy codeStrategy = new RelevanceChecker(new CodeMatcher());
        DozorReporter dozorReporter = new DozorReporter(profile, articleTextFetcher, relevanceStrategy, broadcaster);
        DozorReporter markirovkaDozor  = new DozorReporter(profile, markirovkaArticleTextFetcher, codeStrategy,
                broadcaster);
        CrptFeedMonitor crptFeedMonitor = new CrptFeedMonitor(httpTextFetcher,
                new SeenStore("seen-releases.txt"), dozorReporter);
        MarkirovkaFeedMonitor markirovkaFeedMonitor = new MarkirovkaFeedMonitor(httpTextFetcher,
                new SeenStore("seen-markirovka.txt"), markirovkaDozor, profile);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TelegramReceiver telegramReceiver = new TelegramReceiver(token, objectMapper);
        OffsetStore offsetStore = new OffsetStore("tg-offset.txt");
        SeenStore welcomedStore = new SeenStore("welcomed.txt");
        SubscriberMonitor subscriberMonitor = new SubscriberMonitor(telegramReceiver, offsetStore, subscriberStore,
                onboardingReporter,welcomedStore);

        // Дирижёр получает подписки и всех помощников — и запускает процесс.
        MonitorRunner runner = new MonitorRunner(subscriptions, httpTextFetcher, seenStore, broadcaster);

        scheduler.scheduleWithFixedDelay (
                () -> {
                    try {
                        System.out.println("Тик дозора: " + java.time.LocalTime.now());
                        markirovkaFeedMonitor.run();
//                        crptFeedMonitor.run();
                    } catch (Exception e) {
                        System.out.println("Дозор упал: " + e.getMessage());
                    }
                },
                0, 24, TimeUnit.HOURS);

        scheduler.scheduleWithFixedDelay (
                () -> {
                    try {
                        System.out.println("Тик приёмника: " + java.time.LocalTime.now());
                        subscriberMonitor.run();
                    } catch (Exception e) {
                        System.out.println("Приёмник упал: " + e.getMessage());
                    }
                },
                0, 2, TimeUnit.SECONDS);
        
        // мониторинг pravo.gov.ru временно отключён, чтобы не шуметь при отладке baseline. Вернуть, когда нужно.
//        runner.run();
    }
}