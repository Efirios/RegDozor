package org.regdozor;

import java.util.List;

/**
 * Контракт «по тексту документа реши, какие товары каталога он затрагивает».
 * Разные источники дают РАЗНЫЙ сигнал релевантности, поэтому это интерфейс с двумя реализациями:
 *  - {@link RelevanceChecker} — матч по КОДАМ (ТН ВЭД+ОКПД2), для источников, где коды есть в тексте (markirovka);
 *  - {@link GroupRelevanceStrategy} — матч по НАЗВАНИЮ группы, для релизов честныйзнак (там кодов нет).
 * DozorReporter зависит только от этого контракта и работает с любой реализацией (выбор — при сборке в App).
 */
public interface RelevanceStrategy {
    /**
     * @param text     чистый текст документа
     * @param products весь каталог товаров
     * @return товары, затронутые документом (пустой список, если никого)
     */
    List<Product> findRelevant(String text, Product[] products);
}
