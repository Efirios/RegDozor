package org.regdozor;

/**
 * Одна "подписка" — описание того, ЧТО и как мы мониторим на pravo.gov.ru.
 * Это неизменяемые (immutable) настройки: создали один раз и больше не меняем.
 * Поэтому это record — Java сама генерирует конструктор и методы-геттеры
 * (name(), baseUrl(), maxPages()).
 *
 * @param name     человекочитаемое имя подписки (для логов), напр. "GovMark"
 * @param baseUrl  URL поискового запроса к pravo.gov.ru с параметром index=1
 * @param maxPages сколько страниц выдачи пролистать (пагинация)
 */
public record Subscription(String name, String baseUrl, int maxPages) {

}
