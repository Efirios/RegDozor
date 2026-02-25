package org.bizassistant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceTextReader {

    // Сейчас метод статический, чтобы проще вызывать на старте.
    // Позже, когда появятся настройки/несколько источников, возможно сделаем экземплярный класс.
    public static String read() {
        // Имя ресурса в classpath.
        // Файл должен лежать в src/main/resources/pravo_markirovka.html
        String resourceName = "pravo_markirovka.html";

        // Берём classloader, который знает про ресурсы приложения (resources/jar).
        // Это надёжнее, чем читать по абсолютному пути типа C:\..., потому что на сервере
        // путей проекта уже не будет.
        ClassLoader classLoader = ResourceTextReader.class.getClassLoader();

        // Открываем ресурс как поток байтов.
        // Если файла нет, вернётся null (поэтому ниже обязательна проверка).
        InputStream is = classLoader.getResourceAsStream(resourceName);

        if (is == null) {
            // Лучше "упасть" с понятной ошибкой, чем тихо продолжить с null
            // и получить непонятные NPE дальше.
            throw new IllegalArgumentException("NOT FOUND: " + resourceName);
        }

        // try-with-resources гарантирует, что поток будет закрыт автоматически.
        // Это важно: открытые потоки/файлы нельзя "забывать" закрывать.
        try (is) {
            // Читаем весь файл целиком в массив байтов.
            // Для больших файлов иногда читают порциями, но для HTML-страницы это ок.
            byte[] bytes = is.readAllBytes();

            // Декодируем байты в строку (UTF-8).
            // Теперь это обычный текст HTML.
            return new String(bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            // Для MVP можно заворачивать в RuntimeException, чтобы видеть стек и причину.
            // Позже сделаем аккуратнее (своё исключение/логирование).
            throw new RuntimeException(e);
        }
    }
}