package org.regdozor.match;

import org.jsoup.Jsoup;

/**
 * Из текста статьи КоАП достаёт готовую ЦИТАТУ штрафа для нужного субъекта — чистым текстом.
 *
 * Вход: HTML статьи (его даёт {@link KoapArticleLocator}) + номер части (из таблицы обязанностей) +
 * субъект («на должностных лиц» для ИП). Выход: фрагмент ЖИВОЙ редакции про штраф этого субъекта,
 * без тегов, суммы словами — как в законе, без нашей трактовки.
 *
 * «Луковица» из трёх методов:
 * <pre>
 *   penaltyFor (наружу)  — по (часть, субъект) дать цитату штрафа;
 *     ├ cutPart          — вырезать нужную ЧАСТЬ статьи (нанесение → ч.1, не ч.3/4-алкоголь);
 *     └ subjectPenalty   — в части найти абзац субъекта и очистить от тегов (jsoup).
 * </pre>
 *
 * Место в цепочке: таблица (часть) + локатор (текст статьи) + профиль (субъект) → ЭТОТ класс → сборка алерта.
 * Право сам НЕ выводит — цитирует живой текст; сумму в число НЕ парсит (суммы словами).
 */
public class KoapPenaltyExtractor {
    /**
     * Вырезает ТЕЛО нужной части статьи — от «{@code >N.}» до «{@code >N+1.}». {@code null} → вся
     * статья (частей нет, напр. 15.12¹).
     *
     * ⚠️ Метка части — {@code ">" + part + "."} БЕЗ пробела: после «N.» в тексте КоАП стоит
     *    НЕРАЗРЫВНЫЙ пробел (U+00A0), а не обычный (0x20) — с пробелом в метке {@code indexOf}
     *    промахнулся бы МОЛЧА. Точка отделяет номер, пробел не включаем.
     */
    private String cutPart(String articleHtml, String part) {
        if (part == null) {
            return articleHtml;
        }

        int start = articleHtml.indexOf(">" + part + ".");
        if (start == -1) {
            throw new IllegalStateException("Кусок части " + part + " не найден, разметка/номер могли поменяться");
        }

        String indexPart = String.valueOf(Integer.parseInt(part) + 1);

        int end = articleHtml.indexOf(">" + indexPart + ".", start);
        if (end == -1) {
            end = articleHtml.length();
        }

        return articleHtml.substring(start, end);
    }

    /**
     * В тексте части находит абзац субъекта («на должностных лиц …») до границы «{@code ; на}»
     * (следующий субъект) и возвращает его ЧИСТЫМ текстом.
     *
     * Сперва снимает теги через {@code Jsoup.parse(partHtml).text()} — заодно НОРМАЛИЗУЕТ пробелы
     * (неразрывные становятся обычными), так что дальше про U+00A0 думать не надо. Потом режет от
     * субъекта до «; на».
     */
    private String subjectPenalty(String partHtml, String subject) {
        String text = Jsoup.parse(partHtml).text();

        int start = text.indexOf(subject);
        if (start == -1) {
            throw new IllegalStateException("Фрагмент субъекта \"" + subject +
                    "\" не существует");
        }

        int boundary = text.indexOf("; на", start);
        if (boundary == -1) {
            throw new IllegalStateException("Фрагмент субъекта \"" + subject +
                    "\" не найден в части — формулировка/разметка могла измениться");
        }

        return text.substring(start, boundary);
    }

    /**
     * По (часть, субъект) отдаёт готовую цитату штрафа из статьи — единственная дверь наружу.
     *
     * Сцепляет двух помощников: сперва вырезает нужную часть, потом из неё — абзац субъекта.
     *
     * @param articleHtml текст статьи КоАП в HTML (от {@link KoapArticleLocator})
     * @param part        номер части ("1") или {@code null}, если статья без частей (напр. 15.12¹)
     * @param subject     кого штрафуют, дословно из текста: «на должностных лиц» (ИП) / «на юридических лиц»
     * @return фрагмент про штраф субъекта, чистым текстом (суммы словами)
     */
    public String penaltyFor(String articleHtml, String part, String subject) {
        String partHtml = cutPart(articleHtml, part);
        return subjectPenalty(partHtml, subject);
    }
}
