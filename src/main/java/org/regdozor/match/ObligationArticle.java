package org.regdozor.match;

/**
 * Одна строка таблицы «обязанность → статья КоАП»: указатель, КУДА смотреть за риском.
 *
 * Читается из obligations.json (по одной записи на json-объект). Сам текст статьи и штраф НЕ хранит —
 * только координаты: {@code baseNumber}+{@code superscript} ведут в {@link KoapArticleLocator},
 * {@code part} — в будущий шаг цитирования нужного абзаца.
 *
 * @param group       товарная группа/домен ("одежда") — часть ключа поиска
 * @param obligation  обязанность ("нанесение" / "ГИС МТ") — часть ключа поиска
 * @param baseNumber  базовый номер статьи ("15.12") → в локатор
 * @param superscript надстрочник (0 — нет, 1 — ¹, 2 — ²) → в локатор
 * @param part        номер части статьи ("1"); {@code null}, если статья без деления на части (напр. 15.12¹)
 */
public record ObligationArticle(String group, String obligation, String baseNumber, int superscript, String part) {
}
