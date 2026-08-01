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
     * Собирает цитату штрафа для субъекта из ДВУХ кусков и отдаёт чистым текстом.
     *
     * Санкция устроена так: «влечёт [предупреждение или] наложение штрафа на &lt;субъект1&gt; …; на &lt;субъект2&gt; …».
     * Тип санкции стоит ОДИН раз в начале, амаунты субъектов — списком после. Для не-первого субъекта
     * (юрлицо) между санкцией и его амаунтом вклиниваются чужие — поэтому берём ДВА куска и склеиваем:
     * (1) тип санкции — от «влечет» до конца слова «штрафа»; (2) амаунт субъекта — от {@code subject}
     * до конца его клаузы (ближайшее из «; на» и «.»; у последнего субъекта «; на» нет → берём «.»).
     *
     * ⚠️ Сперва {@code Jsoup.parse(partHtml).text()} снимает теги и НОРМАЛИЗУЕТ пробелы (неразрывные →
     *    обычные), поэтому про U+00A0 тут думать не надо.
     */
    private String subjectPenalty(String partHtml, String subject) {
        // снимаем теги (заодно неразрывные пробелы → обычные) — дальше работаем с чистым текстом
        String text = Jsoup.parse(partHtml).text();

        // --- кусок 1: тип санкции (один на всех субъектов) ---
        int startSanction = text.indexOf("влечет");   // начало клаузы штрафа
        if (startSanction == -1) {                     // санкции нет → разметка/формулировка изменилась
            throw new IllegalStateException("Фрагмент субъекта \"влечёт\" не существует");
        }

        int endSanction = text.indexOf("штрафа");      // слово «штрафа» в «наложение … штрафа»
        if (endSanction == -1) {
            throw new IllegalStateException("Фрагмент субъекта \"штрафа\" не существует");
        }

        // +длина слова, т.к. substring правый край не включает → «штрафа» войдёт целиком
        String sanction  = text.substring(startSanction, endSanction + "штрафа".length());

        // --- кусок 2: амаунт нужного субъекта ---
        int start = text.indexOf(subject);             // начало клаузы субъекта («на должностных лиц» …)
        if (start == -1) {                             // такого субъекта в тексте нет
            throw new IllegalStateException("Фрагмент субъекта \"" + subject +
                    "\" не существует");
        }

        int nextSubject = text.indexOf("; на", start);   // следующий субъект (если есть)
        int sentenceEnd = text.indexOf(".", start);      // конец предложения санкции
        int boundary;

        if (nextSubject == -1) {                         // следующего субъекта нет (напр. юрлицо — последнее)
            boundary = sentenceEnd;                      // → режем до точки
        } else {
            boundary = Math.min(nextSubject, sentenceEnd); // оба есть → берём раньшее
        }

        if (boundary == -1) {                          // не нашли ни «; на», ни «.» — маловероятно
            throw new IllegalStateException("Фрагмент субъекта \"" + subject +
                    "\" не найден в части — формулировка/разметка могла измениться");
        }

        String subjectAmount  = text.substring(start, boundary);   // «на <субъект> … рублей …»

        return sanction + " " + subjectAmount;                  // тип санкции + пробел + амаунт субъекта
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
