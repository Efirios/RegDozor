package org.regdozor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * «Приёмник»: зеркало TelegramNotifier — не шлёт, а СПРАШИВАЕТ у Telegram новые обновления
 * (метод Bot API getUpdates) и отдаёт их уже разобранными в {@link GetUpdatesResponse}.
 * Long polling: параметр timeout=10 — Telegram держит соединение до 10 сек, если обновлений нет.
 * Он ДОЛЖЕН быть меньше HTTP-таймаута (20 сек), иначе наш клиент оборвал бы связь раньше ответа.
 * Параметр offset подтверждает обработанное: всё, что ниже него, Telegram больше не отдаёт.
 *
 * ⚠️ БЕЗОПАСНОСТЬ (здесь была РЕАЛЬНАЯ утечка — не ломать!): botToken лежит ВНУТРИ url.
 * Поэтому класс делает HTTP-запрос САМ (свой HttpClient), а НЕ через общий HttpTextFetcher —
 * тот подставляет url в тексты ошибок, и при недоступности Telegram токен уходил в лог открытым текстом.
 * Правило: url не должен попадать НИ В ОДНО сообщение об ошибке и ни в одну причину исключения.
 */
public class TelegramReceiver {
    /** Токен бота — секрет. Попадает внутрь url, поэтому url нигде не логируем. */
    private final String botToken;
    private final ObjectMapper objectMapper;
    /** Свой HTTP-клиент (а не общий HttpTextFetcher) — ради безопасности, см. Javadoc класса. */
    private final HttpClient client;

    public TelegramReceiver(String botToken, ObjectMapper objectMapper) {
        if (botToken == null || botToken.isBlank()){
            throw new IllegalStateException("не задана переменная окружения botToken");
        }
        this.botToken = botToken;

        if (objectMapper == null){
            throw new IllegalArgumentException("objectMapper не может быть null!");
        }
        this.objectMapper = objectMapper;

        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    /**
     * Спрашивает у Telegram накопившиеся обновления и отдаёт их разобранными.
     * Вызов ВИСИТ до 10 секунд, если обновлений нет (long polling), и отвечает сразу, если есть.
     * Поэтому «тики» приёмника в логе идут неравномерно: ~2 сек когда есть входящие, ~12 сек когда тихо.
     *
     * @param offset с какого номера отдавать. Одновременно ПОДТВЕРЖДЕНИЕ: всё, что ниже offset,
     *               Telegram считает обработанным и больше никогда не вернёт. 0 = «отдай всё, что есть».
     * @return разобранный ответ (result может быть пустым — это норма)
     */
    public GetUpdatesResponse receive(long offset) {
        String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + offset + "&timeout=10";
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(20)).build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Telegram getUpdates failed: status=" + httpResponse.statusCode() +
                        " body=" + httpResponse.body());
            }
            // ВАЖНО про безопасность: в тексты ошибок НЕ подставляем url — он содержит botToken.
            // Иначе секрет утечёт в логи. Поэтому сообщения общие, без адреса запроса.

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                return objectMapper.readValue(httpResponse.body(), GetUpdatesResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse Telegram getUpdates response", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch Telegram updates", e);
        } catch (InterruptedException e) {
            // Поток прервали во время ожидания ответа. Восстанавливаем «флаг прерывания»
            // (send() его сбрасывает), чтобы вышестоящий код знал о прерывании, и падаем с причиной.
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch Telegram updates", e);
        }
    }
}
