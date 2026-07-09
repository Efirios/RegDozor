package org.regdozor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductLoader {
    ObjectMapper mapper = new ObjectMapper();
    public Product[] load() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = ResourceTextReader.read("products.json");

        try {
            Product[] products = mapper.readValue(json, Product[].class);
            return products;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
