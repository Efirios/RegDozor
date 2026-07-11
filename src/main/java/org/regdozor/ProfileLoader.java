package org.regdozor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Загружает profile.json (ОДИН объект {}) в record Profile через Jackson.
 * Отличие от ProductLoader: readValue(json, Profile.class) — .class, а не [].class (в файле {}, не []).
 */
public class ProfileLoader {

    public Profile load() {
        String json = ResourceTextReader.read("profile.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            Profile profile = mapper.readValue(json, Profile.class);
            return profile;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
