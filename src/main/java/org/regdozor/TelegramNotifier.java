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
 * «Оповещатель»: отправляет готовый текст пользователю в Telegram через Bot API (метод sendMessage).
 * botToken и chatId — это СЕКРЕТЫ; они приходят снаружи (App читает их из переменных окружения),
 * в коде их нет. Единственная задача класса — «дан текст, доставь его в чат».
 */
public class TelegramNotifier {
    /** Токен бота — секрет. Вместе с ним формируется URL запроса, поэтому URL нельзя писать в логи. */
    private final String botToken;
    /** Идентификатор чата-получателя. */
    private final String chatId;
    /** Переиспользуемый HTTP-клиент (один на всё время жизни объекта). */
    private final HttpClient client;

    public TelegramNotifier(String botToken, String chatId) {
        if (botToken == null || botToken.isBlank()){
            throw new IllegalStateException("не задана переменная окружения botToken");
        }
        this.botToken = botToken;

        if (chatId == null || chatId.isBlank()){
            throw new IllegalStateException("не задана переменная окружения chatId");
        }
        this.chatId = chatId;

        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    /**
     * Отправляет сообщение в чат. Текст может содержать HTML-разметку Telegram (теги &lt;b&gt;, &lt;a&gt;…).
     *
     * @param text готовый текст сообщения (напр. собранный AlertFormatter)
     */
    public void send(String text) {
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
