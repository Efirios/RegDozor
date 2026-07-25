package org.regdozor.telegram;

/**
 * Чат внутри {@link Message}. Ради этой записи вся вложенность и разбирается:
 * её {@code id} — тот самый chat_id, который мы кладём в реестр подписчиков и подставляем
 * в sendMessage, когда шлём человеку сообщение.
 *
 * @param id идентификатор чата. ⚠️ ТОЛЬКО long, НЕ int: реальный id 8871801123 — это 8,87 млрд,
 *           а максимум int ≈ 2,15 млрд. С int Jackson просто упадёт при разборе ("out of range").
 *           Правило: все идентификаторы Telegram — long.
 */
public record Chat(long id) {
}
