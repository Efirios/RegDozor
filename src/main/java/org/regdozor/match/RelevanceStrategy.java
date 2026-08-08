package org.regdozor.match;

import org.regdozor.profile.UserProduct;

import java.util.List;

/**
 * Контракт «по тексту документа реши, какие товары ПОЛЬЗОВАТЕЛЯ он затрагивает».
 * Разные источники дают РАЗНЫЙ сигнал релевантности, поэтому это интерфейс с двумя реализациями:
 *  - {@link RelevanceChecker} — матч по КОДАМ (ТН ВЭД+ОКПД2), для источников, где коды есть в тексте (markirovka);
 *  - {@link GroupRelevanceStrategy} — матч по НАЗВАНИЮ группы, для релизов честныйзнак (там кодов нет).
 * DozorReporter зависит только от этого контракта и работает с любой реализацией (выбор — при сборке в App).
 *
 * ⚠️ Товары приходят из ПРОФИЛЯ ({@link org.regdozor.profile.UserProduct}), а не из каталога:
 * ручная курация карточек отменена, человек задаёт только свои коды.
 */
public interface RelevanceStrategy {
    /**
     * @param text         чистый текст документа
     * @param userProducts товары пользователя из профиля (имя + ТН ВЭД + ОКПД2)
     * @return товары, затронутые документом (пустой список, если никого)
     */
    List<UserProduct> findRelevant(String text, List<UserProduct> userProducts);
}
