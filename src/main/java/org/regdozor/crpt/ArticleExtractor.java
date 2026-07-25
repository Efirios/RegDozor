package org.regdozor.crpt;

import org.jsoup.nodes.Document;

/**
 * Контракт «достань чистый текст статьи из уже распарсенного HTML-документа».
 * Извлечение зависит от САЙТА (у каждого своя вёрстка), поэтому вынесено в интерфейс:
 * ArticleTextFetcher делает ОБЩЕЕ (качает HTML + парсит в Document), а КАК достать текст —
 * решает конкретная реализация. Так общий код не знает, честныйзнак это или markirovka.
 * Реализации: {@link CrptReleaseExtractor} (честныйзнак.рф), {@link MarkirovkaReleaseExtractor} (markirovka.ru).
 */
public interface ArticleExtractor {
    /**
     * @param doc распарсенный Jsoup-документ страницы
     * @return чистый текст статьи (без меню/шапки/шума — что именно отсечь, знает реализация)
     */
    String extractText(Document doc);
}
