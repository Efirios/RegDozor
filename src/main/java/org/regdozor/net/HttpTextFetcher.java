package org.regdozor.net;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

import static java.lang.Math.min;

/**
 * "Качалка": единственная задача — скачать HTML-страницу по URL и вернуть её как строку.
 * Инкапсулирует всю возню с HTTP: таймауты, редиректы, заголовки, коды ответа.
 * Остальной код (парсер, runner) про HTTP ничего не знает — он просто получает готовый текст.
 */
public class HttpTextFetcher {
    /** Переиспользуемый HTTP-клиент. Создаётся один раз в конструкторе. */
    private final HttpClient client;

    public HttpTextFetcher() {
        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    /**
     * Скачивает страницу по адресу url.
     *
     * @param url адрес страницы (может содержать кириллицу — она будет закодирована в ASCII)
     * @return тело ответа (HTML) как строка в кодировке UTF-8
     * @throws IllegalStateException если сервер ответил кодом, отличным от 200
     * @throws RuntimeException      при сетевой ошибке или прерывании потока
     */
    public String fetch (String url) {
        // URL с кириллицей (напр. Name=маркировки) нельзя слать как есть —
        // переводим его в чистый ASCII-вид (проценты-кодирование).
        URI rawUri = URI.create(url);
        URI asciiUri = URI.create(rawUri.toASCIIString());

        HttpRequest httpRequest = HttpRequest.newBuilder(asciiUri).GET().
                timeout(Duration.ofSeconds(20)).header("User-Agent", "RegDozor/0.1").
                header("Accept", "text/html").build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() == 200) {
                return httpResponse.body();
            } else {
                int status = httpResponse.statusCode();

                String body = httpResponse.body();
                if (body == null) {
                    body = "";
                }

                String contentTypeStr = httpResponse.headers().firstValue("Content-Type").orElse("<none>");

                int max = 500;
                int n = min(max, body.length());

                String snippet = body.substring(0, n);
                snippet = snippet.replace("\r", " ");
                snippet = snippet.replace("\n", " ");
                snippet = snippet.replace("\t", " ");
                snippet = snippet.replaceAll("\\s+", " ");
                snippet = snippet.trim();

                String message = "HTTP " + status + " for " + url +
                        "; Content-Type=" + contentTypeStr +
                        "; body(0.." + n + ")=" + snippet;

                throw new IllegalStateException(message);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while fetching " + url, e);
        }
    }
}
