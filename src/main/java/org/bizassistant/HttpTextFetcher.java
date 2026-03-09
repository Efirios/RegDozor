package org.bizassistant;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

import static java.lang.Math.min;

public class HttpTextFetcher {
    private final HttpClient client;

    public HttpTextFetcher() {
        this.client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1).
                followRedirects(HttpClient.Redirect.ALWAYS).
                connectTimeout(Duration.ofSeconds(10)).
                build();
    }

    public String fetch (String url) {
        URI rawUri = URI.create(url);
        URI asciiUri = URI.create(rawUri.toASCIIString());

        HttpRequest httpRequest = HttpRequest.newBuilder(asciiUri).GET().
                timeout(Duration.ofSeconds(20)).header("User-Agent", "BizAssistant/0.1").
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
