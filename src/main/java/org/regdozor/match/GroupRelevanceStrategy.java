package org.regdozor.match;

import org.regdozor.catalog.Product;
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
    /** Профиль пользователя с терминами его группы. Значение, не «загрузчик». */
    private final Profile profile;

    public GroupRelevanceStrategy(GroupMatcher groupMatcher, Profile profile) {
        if (groupMatcher == null) {
            throw new IllegalArgumentException("groupMatcher не может быть null!");
        }
        this.groupMatcher = groupMatcher;

        if (profile == null) {
            throw new IllegalArgumentException("profile не может быть null!");
        }
        this.profile = profile;
    }

    @Override
    public List<UserProduct> findRelevant(String text, List<UserProduct> userProducts) {
        if (groupMatcher.concernsGroup(text, profile)) {
            return userProducts;
        }
        return List.of();
    }
}
