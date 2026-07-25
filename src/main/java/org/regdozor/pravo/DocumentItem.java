package org.regdozor.pravo;

/**
 * Один документ, вытащенный из поисковой выдачи pravo.gov.ru.
 * Результат работы парсера: неизменяемый "снимок" одной строки выдачи.
 *
 * @param eoNumber    уникальный номер публикации (напр. "0001202401010001").
 *                    Именно по нему мы отличаем "новый" документ от "уже виденного".
 * @param publishDate дата опубликования (как строка с сайта)
 * @param documentUrl ссылка на страницу документа
 * @param pdfUrl      ссылка на PDF-файл документа
 * @param title       заголовок документа
 */
public record DocumentItem(String eoNumber, String publishDate, String documentUrl, String pdfUrl, String title) {
}
