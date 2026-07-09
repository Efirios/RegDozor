package org.regdozor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TelegramNotifier {
    private final String botToken;
    private final String chatId;
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

    public void send(String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(20)).build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Telegram sendMessage failed: status=" + httpResponse.statusCode() +
                        " body=" + httpResponse.body());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send Telegram message", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}
