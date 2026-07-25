package org.regdozor.telegram;

/**
 * Сообщение внутри {@link Update}. В настоящем JSON у него много полей (message_id, from, date, text…),
 * но нам нужен ТОЛЬКО чат — из него берём id, чтобы знать, кому отвечать. Остальное Jackson отбросит.
 *
 * @param chat чат, откуда пришло сообщение
 */
public record Message(Chat chat) {
}
