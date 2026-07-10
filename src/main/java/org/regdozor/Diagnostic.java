package org.regdozor;

/**
 * ВРЕМЕННЫЙ отладочный класс (не часть продукта, удалить при выносе движка "дозора" в боевой код).
 * Прогоняет ядро "дозора" на двух реальных страницах ЦРПТ: fetch чистого текста -> RelevanceChecker.
 * Ожидание: волна-3 (mart-2025) -> [майка], волна-4 (s-1-marta-2026) -> [] (ложный флаг 6104 отсечён ОКПД2).
 */
public class Diagnostic {
    public static void main(String[] args) {
        HttpTextFetcher fetcher = new HttpTextFetcher();
        CodeMatcher matcher = new CodeMatcher();
        RelevanceChecker relevanceChecker = new RelevanceChecker(matcher);
        ArticleTextFetcher articleTextFetcher = new ArticleTextFetcher(fetcher);

        String url1 = "https://markirovka.ru/community/shoes-and-clothes/rasshirenie-perechnya-tovarov-legkoy-" +
                "promyshlennosti-mart-2025";


        String url2 = "https://markirovka.ru/community/shoes-and-clothes/rasshirenie-perechnya-tovarov-legkoy-" +
                "promyshlennosti-s-1-marta-2026";

        Product[] products = new ProductLoader().load();

        System.out.println(relevanceChecker.findRelevant(articleTextFetcher.fetchCleanText(url1), products));
        System.out.println(relevanceChecker.findRelevant(articleTextFetcher.fetchCleanText(url2), products));

    }
}
