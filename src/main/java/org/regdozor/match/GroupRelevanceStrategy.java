package org.regdozor.match;

import org.regdozor.profile.Profile;
import org.regdozor.profile.UserProduct;

import java.util.List;

/**
 * Реализация {@link RelevanceStrategy} «по названию товарной группы» — для релизов честныйзнак.рф,
 * которые описывают изменения НЕ кодами, а именами групп («одежда и бельё»).
 * Логика: если текст относится к группе пользователя (GroupMatcher по Profile) — считаем затронутыми
 * ВСЕ товары профиля; иначе — никого.
 * ⚠️ Допущение «весь ассортимент» верно, пока группа одна; для мультигруппных профилей позже — фильтровать по группе.
 * Profile держим уже загруженным (грузится один раз в App), а не читаем файл на каждый вызов.
 */
public class GroupRelevanceStrategy implements RelevanceStrategy{
    /** Матчер «текст относится к группе?» (нормализация регистра/ё, правило «любой термин»). */
    private final GroupMatcher groupMatcher;

    public GroupRelevanceStrategy(GroupMatcher groupMatcher) {
        if (groupMatcher == null) {
            throw new IllegalArgumentException("groupMatcher не может быть null!");
        }
        this.groupMatcher = groupMatcher;
    }

    @Override
    public List<UserProduct> findRelevant(String text, Profile profile) {
        if (groupMatcher.concernsGroup(text, profile)) {
            return profile.products();
        }
        return List.of();
    }
}
