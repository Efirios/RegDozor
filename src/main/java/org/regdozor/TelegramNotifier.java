package org.regdozor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * «Оповещатель»: отправляет готовый текст в ОДИН чат Telegram через Bot API (метод sendMessage).
 * botToken — СЕКРЕТ, приходит снаружи (App читает его из переменной окружения), в коде его нет.
 * Класс знает только «КАК отправить одному названному чату»; «КОМУ рассылать» решает Broadcaster.
 */
public class TelegramNotifier {
    /** Токен бота — секрет. Вместе с ним формируется URL запроса, поэтому URL нельзя писать в логи. */
    private final String botToken;
    /** Переиспользуемый HTTP-клиент (один на всё время жизни объекта). */
    private final HttpClient client;

    public TelegramNotifier(String botToken) {
        if (botToken == null || botToken.isBlank()){
            throw new IllegalStateException("не задана переменная окружения botToken");
        }
        this.botToken = botToken;

        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    /**
     * Отправляет сообщение в указанный чат. Текст может содержать HTML-разметку Telegram (теги &lt;b&gt;, &lt;a&gt;…).
     *
     * @param chatId идентификатор чата-получателя
     * @param text   готовый текст сообщения (напр. собранный AlertFormatter)
     */
    public void send(String chatId, String text) {
        // link_preview_options={"is_disabled":true} — выключаем «превью» ссылок, чтобы карточка
        // не раздувалась картинками из ссылок. Значение — это JSON, его тоже надо закодировать для URL.
        String linkPreview = URLEncoder.encode("{\"is_disabled\":true}", StandardCharsets.UTF_8);

        // Собираем адрес запроса к Bot API. Внутри — botToken (секрет!) и сам текст.
        // parse_mode=HTML — просим Telegram трактовать теги в тексте как разметку.
        // URLEncoder.encode — экранируем текст, иначе пробелы/кириллица/& поломают URL.
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) + "&parse_mode=HTML" + "&link_preview_options=" +
                linkPreview;

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(20)).build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Telegram sendMessage failed: status=" + httpResponse.statusCode() +
                        " body=" + httpResponse.body());
            }
            // ВАЖНО про безопасность: в тексты ошибок НЕ подставляем url — он содержит botToken.
            // Иначе секрет утечёт в логи. Поэтому сообщения общие, без адреса запроса.
        } catch (IOException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        } catch (InterruptedException e) {
            // Поток прервали во время ожидания ответа. Восстанавливаем «флаг прерывания»
            // (send() его сбрасывает), чтобы вышестоящий код знал о прерывании, и падаем с причиной.
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
