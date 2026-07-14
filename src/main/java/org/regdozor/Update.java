package org.regdozor;

/**
 * ОДНО обновление из ответа getUpdates (элемент массива result).
 *
 * @param update_id номер обновления. Он же — ключ подтверждения: передав Telegram
 *                  offset = (максимальный update_id) + 1, мы говорим «всё, что ниже, обработано»,
 *                  и он больше это не отдаёт. Имя с подчёркиванием — как в JSON (чтобы Jackson сопоставил).
 *                  Тип long, НЕ int: идентификаторы Telegram давно вышли за 2 млрд.
 * @param message   сообщение. ⚠️ МОЖЕТ БЫТЬ null! Не у всякого обновления есть message
 *                  (бывают edited_message, callback_query и др.). Перед обращением к message.chat()
 *                  ОБЯЗАТЕЛЬНА проверка на null, иначе NullPointerException.
 */
public record Update(long update_id, Message message) {
}
