package org.regdozor.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.regdozor.util.ResourceTextReader;

import java.util.LinkedHashMap;
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
        // читаем файл-ресурс obligations.json в одну строку (пока это просто текст json)
        String json = ResourceTextReader.read("obligations.json");
        // берём «переводчик» Jackson — он превращает json-текст в Java-объекты
        ObjectMapper objectMapper = new ObjectMapper();
        // настраиваем переводчик: встретит в json лишнее поле, которого нет в record'е, — не падать, а пропустить
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            // переводим строку json в массив записей: json-массив [ ] → ObligationArticle[]
            ObligationArticle[] rows = objectMapper.readValue(json, ObligationArticle[].class);
            // заводим пустую карту-справочник: слева контракт Map, справа конкретная реализация.
            // ⚠️ ИМЕННО LinkedHashMap, а не HashMap: он помнит ПОРЯДОК ДОБАВЛЕНИЯ, а добавляем мы в
            // порядке json. Значит порядок записей в obligations.json = порядок обязанностей в алерте
            // (нанесение перед ГИС МТ). У HashMap порядка нет вовсе, и алерт выдавал бы их вразнобой.
            Map<ObligationKey, ObligationArticle> table = new LinkedHashMap<>();

            // проходим по каждой записи массива
            for (ObligationArticle row : rows) {
                // собираем составной ключ из двух полей записи: (группа, обязанность)
                ObligationKey key = new ObligationKey(row.group(), row.obligation());
                // кладём в карту: под этим ключом лежит вся запись row
                table.put(key, row);
            }

            // отдаём заполненную карту наружу
            return table;
        // сюда попадаем, если json битый и readValue кинул проверяемое JsonProcessingException
        } catch (JsonProcessingException e) {
            // перебрасываем как непроверяемое RuntimeException, обернув причину (битый ресурс не починить на лету)
            throw new RuntimeException(e);
        }
    }
}
