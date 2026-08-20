package org.regdozor.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.regdozor.util.ResourceTextReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Читает groups.json и отдаёт справочник «группа → {@link GroupInfo}» для поиска по названию группы.
 *
 * Брат {@code ObligationTableLoader}: json-массив читается в {@code GroupInfo[]} (Jackson), затем
 * ИНДЕКСИРУЕТСЯ в карту — ключом берётся поле {@code group} каждой записи.
 *
 * ⚠️ Ключ здесь ПРОСТОЙ (строка), в отличие от {@code ObligationKey}: там искали по паре
 * «группа + обязанность», тут достаточно названия группы. И реализация — обычный {@code HashMap},
 * а не {@code LinkedHashMap}: по справочнику не ходят, к нему обращаются по ключу, порядок не нужен.
 *
 * ⚠️ Неизвестная группа даёт {@code null} — тот, кто спрашивает, обязан это проверить. Случится это
 * при онбординге, если человек назовёт группу, которой в справочнике нет.
 *
 * Сам НЕ решает данные (groups.json — зона пользователя) и НЕ ищет по справочнику (это {@code get}
 * у потребителя).
 */
public class GroupInfoLoader {
    /**
     * Загружает groups.json в карту «название группы → запись».
     *
     * ⚠️ Имя файла в {@code read(...)} должно ТОЧНО совпадать с именем ресурса — опечатку компилятор
     * не поймает, промах виден только на запуске.
     *
     * @return карта: название группы → раздел markirovka и термины
     * @throws RuntimeException если json не разобрался (обёрнутый {@code JsonProcessingException})
     */
    public Map<String, GroupInfo> load() {
        String json = ResourceTextReader.read("groups.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            GroupInfo[] rows = objectMapper.readValue(json, GroupInfo[].class);
            Map<String, GroupInfo> table = new HashMap<>();

            for (GroupInfo row : rows) {
              String key = row.group();
              table.put(key, row);
            }

            return table;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
