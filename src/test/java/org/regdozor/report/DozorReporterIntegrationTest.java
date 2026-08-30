package org.regdozor.report;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.regdozor.match.*;
import org.regdozor.net.HttpTextFetcher;
import org.regdozor.operator.*;
import org.regdozor.pravo.PravoEbpiTextFetcher;
import org.regdozor.profile.Profile;
import org.regdozor.profile.ProfileStore;
import org.regdozor.profile.Subject;
import org.regdozor.profile.UserProduct;
import org.regdozor.store.SeenStore;
import org.regdozor.telegram.TelegramNotifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DozorReporterIntegrationTest {

    public static class FakeHttpTextFetcher extends HttpTextFetcher {
        String html;

        public FakeHttpTextFetcher(String html) {
            this.html = html;
        }

        @Override
        public String fetch (String url) {
            return this.html;
        }
    }

    public static class FakeTelegramNotifier extends TelegramNotifier {
        List<String> chatIds = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        public FakeTelegramNotifier() {
            super("lubaya stroka");
        }

        public List<String> getChatIds() {
            return chatIds;
        }

        public List<String> getTexts() {
            return texts;
        }

        @Override
        public void send(String chatId, String text) {
            chatIds.add(chatId);
            texts.add(text);
        }
    }

    public static class FakeKoapRisk extends KoapRisk {
        int count = 0;

        public FakeKoapRisk(Map<ObligationKey, ObligationArticle> table, PravoEbpiTextFetcher pravoEbpiTextFetcher,
                            KoapArticleLocator koapArticleLocator, KoapPenaltyExtractor koapPenaltyExtractor) {
            super(table, pravoEbpiTextFetcher, koapArticleLocator, koapPenaltyExtractor);
        }

        public int getCount() {
            return count;
        }

        @Override
        public List<ObligationRisk> risksForGroup(String group, Subject subject) {
            count += 1;

            List<ObligationRisk> obligationRisks = List.of(new ObligationRisk("Продажа товар без товара",
                    "Влечёт получение пи****ей"));

            return obligationRisks;
        }
    }

    @TempDir
    Path tempDir;

    private static final String ARTICLE_HTML = """
            <html>
            <head><title>Расширение перечня товаров лёгкой промышленности</title></head>
            <body>
              <h1>Расширение перечня товаров лёгкой промышленности</h1>

              <p>С 1 марта 2026 года в перечень включены товары
                 с кодами ТН ВЭД 6109 и ОКПД2 14.14.30.</p>

              <h3>Основные этапы маркировки</h3>
              <p><b>С 1 марта 2026 года</b> — старт обязательной маркировки.</p>
              <p><b>С 1 июля 2026 года</b> — запрет выпуска таможенными органами.</p>

              <blockquote>*Остатки товаров — это товары во владении на дату старта.</blockquote>

              <h3>Как промаркировать остатки?</h3>
              <p>Зарегистрируйтесь в системе маркировки.</p>
            </body>
            </html>
            """;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(){
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("Сообщение получает только подписчик, чьи коды встретились в статье")
    void onlyMatchedSubscriberGetsMessage() throws Exception {
        FakeHttpTextFetcher fakeHttpTextFetcher = new FakeHttpTextFetcher(ARTICLE_HTML);
        FakeTelegramNotifier fakeTelegramNotifier = new FakeTelegramNotifier();

        ObligationTableLoader obligationTableLoader = new ObligationTableLoader();
        Map<ObligationKey, ObligationArticle> table = obligationTableLoader.load();
        PravoEbpiTextFetcher pravoEbpiTextFetcher = new PravoEbpiTextFetcher(fakeHttpTextFetcher,objectMapper);
        KoapArticleLocator koapArticleLocator = new KoapArticleLocator();
        KoapPenaltyExtractor koapPenaltyExtractor = new KoapPenaltyExtractor();
        FakeKoapRisk fakeKoapRisk = new FakeKoapRisk(table, pravoEbpiTextFetcher, koapArticleLocator, koapPenaltyExtractor);

        ProfileStore profileStore = new ProfileStore(objectMapper, tempDir);
        MarkirovkaReleaseExtractor markirovkaReleaseExtractor = new MarkirovkaReleaseExtractor();
        ArticleTextFetcher fakeArticleTextFetcher = new ArticleTextFetcher(fakeHttpTextFetcher, markirovkaReleaseExtractor);
        RelevanceStrategy codeStrategy = new RelevanceChecker(new CodeMatcher());
        MarkirovkaStagesExtractor markirovkaStagesExtractor = new MarkirovkaStagesExtractor();
        AlertBuilder alertBuilder = new AlertBuilder();
        MarkirovkaAlertStrategy markirovkaAlertStrategy = new MarkirovkaAlertStrategy(markirovkaStagesExtractor,
                alertBuilder);
        SeenStore seenStore = new SeenStore("chat-ids.txt", tempDir);

        DozorReporter dozorReporter = new DozorReporter(fakeArticleTextFetcher, codeStrategy, fakeTelegramNotifier,
                markirovkaAlertStrategy, seenStore, profileStore, fakeKoapRisk);

        Files.writeString(tempDir.resolve("chat-ids.txt"), "111\n222");

        List<UserProduct> productsOfFirst = List.of(new UserProduct("Майка женская", "6109",
                "14.14.30", "одежда"));
        Profile profile1 = new Profile(
                List.of("лёгкая промышленность"),
                List.of("shoes-and-clothes"),
                Subject.IP,
                productsOfFirst);

        profileStore.save("111", profile1);

        List<UserProduct> productsOfSecond = List.of(new UserProduct("Майка женская", "8517",
                "26.30.11", "одежда"));
        Profile profile2 = new Profile(
                List.of("лёгкая промышленность"),
                List.of("shoes-and-clothes"),
                Subject.LEGAL,
                productsOfSecond);

        profileStore.save("222", profile2);

        dozorReporter.run("https://xn--80ajghhoc2aj1c8b.xn--p1ai/info/releasenotes/chto-novogo-v-sisteme-s-26-01-2026-po-30-01-2026/");

        assertEquals(1, fakeTelegramNotifier.getChatIds().size());
        assertEquals("111", fakeTelegramNotifier.getChatIds().getFirst());
        assertTrue(fakeTelegramNotifier.getTexts().getFirst().contains("Майка женская"));
    }
}
