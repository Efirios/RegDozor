package org.regdozor.crpt;

import org.jsoup.nodes.Document;

/**
 * Реализация {@link ArticleExtractor} для markirovka.ru.
 * Сначала УДАЛЯЕТ из документа блоки комментариев (любой класс, где встречается "comment") —
 * это вопросы пользователей (шум, мешает матчингу кодов), потом берёт текст всего оставшегося.
 * Замечание: remove() меняет переданный Document (мутирует дерево) — здесь это ок, документ одноразовый.
 */
public class MarkirovkaReleaseExtractor implements ArticleExtractor{
    @Override
    public String extractText(Document doc) {
        doc.select("[class*=comment]").remove();
        return doc.text();
    }
}
