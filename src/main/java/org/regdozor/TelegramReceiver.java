package org.regdozor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TelegramReceiver {
    private final String botToken;
    private final ObjectMapper objectMapper;
    private final HttpTextFetcher httpTextFetcher;

    public TelegramReceiver(String botToken, ObjectMapper objectMapper, HttpTextFetcher httpTextFetcher) {
        if (botToken == null || botToken.isBlank()){
            throw new IllegalStateException("не задана переменная окружения botToken");
        }
        this.botToken = botToken;

        if (objectMapper == null){
            throw new IllegalArgumentException("objectMapper не может быть null!");
        }
        this.objectMapper = objectMapper;

        if (httpTextFetcher == null){
            throw new IllegalArgumentException("httpTextFetcher не может быть null!");
        }
        this.httpTextFetcher = httpTextFetcher;
    }

    public GetUpdatesResponse receive(long offset) {
        String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + offset + "&timeout=10";
        String json = httpTextFetcher.fetch(url);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objectMapper.readValue(json, GetUpdatesResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
