package org.regdozor;

/**
 * Чем грозит нарушение одной обязанности: статья, последствие, штраф.
 *
 * Живёт ВНУТРИ Obligation, а не Product — у разных обязанностей разные статьи КоАП.
 * Может быть null, если норма ещё не проверена.
 *
 * @param article     статья (напр. «ст. 15.12 КоАП РФ (ч.1, ч.2)»)
 * @param consequence вид последствия («штраф + конфискация» и т.п.)
 * @param fine        размер штрафа С УКАЗАНИЕМ субъекта (у ИП/должностного/юрлица суммы разные!)
 * @param sourceUrl   ссылка на текст статьи (КоАП, не ППРФ)
 * @param verifiedOn  дата, когда норму сверили с первоисточником (у каждого утверждения СВОЯ)
 */
public record Risk(String article, String consequence, String fine, String sourceUrl, String verifiedOn) {
}
