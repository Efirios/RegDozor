package org.regdozor.crpt;

import org.jsoup.nodes.Document;

/**
 * Реализация {@link ArticleExtractor} для честныйзнак.рф (лента релизов «Что нового в системе»).
 * Берёт ТОЛЬКО тело статьи — элемент с классом .text-par-lh-big.
 * Почему не весь текст страницы: в меню/шапке/хештегах перечислены ВСЕ товарные группы,
 * и матчинг по группе давал бы ложные срабатывания. Тело статьи — только по существу.
 */
public class CrptReleaseExtractor implements ArticleExtractor{
    @Override
    public String extractText(Document doc) {
        return doc.select(".text-par-lh-big").text();
    }
}
