package org.regdozor.match;

import org.regdozor.profile.Profile;
import org.regdozor.profile.UserProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * "Проверка релевантности": какие товары ПОЛЬЗОВАТЕЛЯ (из профиля) затронуты документом.
 * Правило (официальное, сноска markirovka): маркируемость определяется ТН ВЭД + ОКПД2 ОДНОВРЕМЕННО.
 * Товар релевантен ⟺ в тексте найдены ВСЕ его коды (found.size() == codes.size()), а не «хотя бы один» —
 * это отсекает ложные срабатывания по широкому 4-значному коду (юбки 6104 vs СИЗ волны-4).
 * okpd2 может быть null → матчим по одному ТН ВЭД (грубый флаг).
 */
public class RelevanceChecker implements RelevanceStrategy{
    private final CodeMatcher matcher;

    public RelevanceChecker(CodeMatcher matcher) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher не может быть null!");
        }
        this.matcher = matcher;
    }

    /**
     * Товар релевантен ⟺ в тексте найдены ВСЕ его коды (а не «хотя бы один»).
     * Правило «все» отсекает ложные срабатывания по широкому 4-значному ТН ВЭД:
     * позиция 6104 накрывает и юбки клиента, и спецодежду/СИЗ из другой волны —
     * различает их только второй код, ОКПД2.
     */
    @Override
    public List<UserProduct> findRelevant(String text, Profile profile) {
        List<UserProduct> relevant = new ArrayList<>();

        for (UserProduct p : profile.products()) {
            // Собираем коды товара, по которым будем искать: ТН ВЭД обязательно, ОКПД2 — если задан.
            List<String> codes = new ArrayList<>();

            codes.add(p.tnved());

            if (p.okpd2() != null) {
                codes.add(p.okpd2());
            }

            List<String> found = matcher.findMentioned(text, codes);

            // Ключевое условие: найдено СТОЛЬКО ЖЕ, сколько искали → значит найдены ВСЕ.
            // Если ОКПД2 не задан (null), ищем по одному коду — это грубый флаг, зато не молчим.
            if (found.size() == codes.size()) {
                relevant.add(p);
            }
        }
        return relevant;
    }
}
