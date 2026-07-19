package org.regdozor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ищет в тексте коды из списка и возвращает найденные.
 *
 * «Сопоставитель кодов»: чистая функция без ввода-вывода (простой contains). Не знает и не должен знать,
 * ТН ВЭД перед ним или ОКПД2, из какого источника текст. Легко тестируется и переиспользуется.
 */
public class CodeMatcher {
    /**
     * @param text  очищенный текст документа
     * @param codes искомые коды (ТН ВЭД и/или ОКПД2)
     * @return подсписок codes, которые найдены в text
     */
    public List<String> findMentioned(String text, List<String> codes) {
        List<String> mentioned = new ArrayList<>();

        for (String code : codes) {
            if (text.contains(code)) {
                mentioned.add(code);
            }
        }
        return mentioned;
    }
}
