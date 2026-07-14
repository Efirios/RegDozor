package org.regdozor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Загружает каталог товаров из products.json в массив Product[].
 * Использует Jackson (библиотека чтения JSON): ObjectMapper по совпадению имён полей JSON
 * и полей record превращает текст в объекты. Файл — МАССИВ [], поэтому целевой тип Product[].class
 * (сравни с ProfileLoader, где один объект {} и .class).
 */
public class ProductLoader {
    // ObjectMapper — «переводчик» JSON ⇄ объекты Java. Один на класс, переиспользуется.
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Читает products.json и превращает его в массив товарных карточек.
     *
     * @return весь каталог товаров пользователя
     */
    public Product[] load() {
        String json = ResourceTextReader.read("products.json");
        // FAIL_ON_UNKNOWN_PROPERTIES=false: не падать, если в JSON есть поле, которого нет в record
        // (напр. служебное "_note" с пометками). По умолчанию Jackson на такое ругается.
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        try {
            // readValue: текст JSON -> массив Product. Тип цели передаём вторым аргументом.
            Product[] products = mapper.readValue(json, Product[].class);
            return products;
        } catch (JsonProcessingException e) {
            // Битый/несовпадающий JSON — падаем с причиной, чтобы сразу увидеть, что не так.
            throw new RuntimeException(e);
        }
    }
}
