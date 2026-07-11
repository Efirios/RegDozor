package org.regdozor;

/**
 * Сигнал релевантности по НАЗВАНИЮ группы (для релизов ЦРПТ, где нет кодов ТН ВЭД).
 * concernsGroup: нормализует текст и термины (toLowerCase + ё→е против ловушек регистра и буквы ё)
 * и возвращает true, если найден ЛЮБОЙ термин группы (в отличие от RelevanceChecker: там «найдены ВСЕ коды»).
 * ВАЖНО: подавать сюда ТОЛЬКО тело статьи (иначе меню/шапка с названиями всех групп → ложные срабатывания).
 */
public class GroupMatcher {
    public boolean concernsGroup(String text, Profile profile) {
        String normalText = text.toLowerCase().replace('ё', 'е');

        for (String term : profile.groupTerms()) {
            String normTerm = term.toLowerCase().replace('ё', 'е');

            if (normalText.contains(normTerm)) {
                return true;
            }
        }

        return false;
    }
}
