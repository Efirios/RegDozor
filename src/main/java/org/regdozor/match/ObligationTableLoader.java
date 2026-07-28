package org.regdozor.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.regdozor.util.ResourceTextReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Читает obligations.json и отдаёт готовую {@link Map} для поиска статьи по паре (группа + обязанность).
 *
 * Брат {@code ProfileLoader}/{@code ProductLoader}, но с добавкой: json-массив сперва читается в
 * {@link ObligationArticle}[] (Jackson), а потом ИНДЕКСИРУЕТСЯ в {@code Map<ObligationKey, ObligationArticle>} —
 * ключ собирается из двух полей каждой записи. Так поиск идёт за O(1) по точной паре, а не перебором списка.
 *
 * Сам НЕ решает данные (это obligations.json — зона пользователя) и НЕ ищет по таблице
 * (это {@code map.get(...)} у потребителя).
 */
public class ObligationTableLoader {
    /**
     * Загружает obligations.json в карту «(группа, обязанность) → запись».
     *
     * Две половины: (1) прочитать json-массив в {@code ObligationArticle[]} (как в ProductLoader);
     * (2) пройти циклом и на каждую запись положить в карту ключ {@code new ObligationKey(group, obligation)} → запись.
     *
     * ⚠️ Имя файла в {@code read(...)} должно ТОЧНО совпадать с именем ресурса — опечатку компилятор
     *    не ловит, промах виден только на запуске.
     *
     * @return карта: ключ {@link ObligationKey}(group, obligation) → вся строка {@link ObligationArticle}
     * @throws RuntimeException если json не разобрался (обёрнутый {@code JsonProcessingException})
     */
    public Map<ObligationKey, ObligationArticle> load() {
        String json = ResourceTextReader.read("obligations.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            ObligationArticle[] rows = objectMapper.readValue(json, ObligationArticle[].class);
            Map<ObligationKey, ObligationArticle> table = new HashMap<>();

            for (ObligationArticle row : rows) {
                ObligationKey key = new ObligationKey(row.group(), row.obligation());
                table.put(key, row);
            }

            return table;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
