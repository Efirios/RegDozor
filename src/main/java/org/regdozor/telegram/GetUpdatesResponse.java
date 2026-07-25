package org.regdozor.telegram;

import java.util.List;

/**
 * ВЕРШИНА ответа Telegram на getUpdates. Одна из четырёх записей, повторяющих вложенность JSON:
 * GetUpdatesResponse -> {@link Update} -> {@link Message} -> {@link Chat}.
 * Каждая запись = один уровень вложенности; берём только те поля, что нужны, остальное Jackson
 * отбрасывает (FAIL_ON_UNKNOWN_PROPERTIES=false в TelegramReceiver).
 *
 * Реальный ответ выглядит так (лишние поля опущены):
 * <pre>
 * { "ok": true,
 *   "result": [ { "update_id": 540637246,
 *                 "message": { "chat": { "id": 1234567890 } } } ] }
 * </pre>
 *
 * Имена компонентов совпадают с именами полей в JSON — поэтому аннотации @JsonProperty не нужны.
 *
 * @param ok     признак успеха от Telegram
 * @param result массив обновлений (может быть пустым — при long polling это норма: никто не писал)
 */
public record GetUpdatesResponse(Boolean ok, List<Update> result) {
}
