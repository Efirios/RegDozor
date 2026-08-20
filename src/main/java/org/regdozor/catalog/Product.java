package org.regdozor.catalog;

import java.util.List;

/**
 * «Товарная карточка» — одна запись каталога products.json, привязанная к коду ТН ВЭД.
 * Отвечает на вопрос «нужна ли маркировка ЭТОГО товара и с какой даты».
 * Не путать с «процессной карточкой» (регистрация в ЧЗ, заказ кодов…) — та привязана к РОЛИ, а не к товару.
 *
 * record = неизменяемые данные: Java сама делает конструктор и геттеры (code(), okpd2()…),
 * а Jackson (см. ProductLoader) заполняет поля из JSON по совпадению имён.
 *
 * @param tnved            код ТН ВЭД (напр. "6109"). Иерархичен: 4/6/10 знаков, глубина важна
 * @param okpd2           код ОКПД2 (напр. "14.14.30"); вместе с ТН ВЭД снимает неоднозначность 4-значного кода. Может быть null
 * @param officialName    официальное название позиции из перечня (для человека)
 * @param productNames    как товар называет сам клиент ("Майка женская") — список, т.к. синонимов может быть много
 * @param category        внутренняя группировка ("трикотаж-бельё") — для нас, не из закона
 * @param markingRequired ТРИ состояния: true (подлежит) / false (не подлежит) / null (не определено).
 *                        Поэтому тип Boolean (объект, может быть null), а не примитив boolean
 * @param obligations     список обязанностей по этому товару (нанесение, внесение сведений…), у каждой своя дата и риск
 * @param codeSource      откуда взят код (декларация/сертификат клиента) — для прослеживаемости
 * @param verifiedOn      дата, когда коды и сроки сверили с первоисточником
 */
public record Product(String tnved, String okpd2, String officialName, List<String> productNames, String category,
                      Boolean markingRequired, List<Obligation> obligations, String codeSource,
                      String verifiedOn) {
}
