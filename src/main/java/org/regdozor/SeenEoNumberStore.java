package org.regdozor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Память" приложения между запусками.
 * Хранит множество eoNumber-ов документов, которые мы УЖЕ видели раньше,
 * в текстовом файле data/seen-eonumbers.txt (по одному номеру на строку).
 *
 * Благодаря этому классу приложение при каждом запуске понимает,
 * какие документы новые, а какие уже показывались.
 * Роли методов зеркальные: load() читает диск -> память, save() пишет память -> диск.
 */
public class SeenEoNumberStore {
    /** Путь к файлу-хранилищу. Один и тот же на весь срок жизни объекта -> final. */
    private final Path filePath;

    public SeenEoNumberStore() {
        filePath = Path.of("data", "seen-eonumbers.txt");
    }

    /**
     * Читает из файла множество уже виденных номеров.
     * Если файла ещё нет (самый первый запуск) — возвращает пустое множество.
     * Пустые строки и пробелы по краям отбрасываются.
     *
     * @return множество eoNumber-ов, известных из прошлых запусков
     */
    public Set<String> load(){
        if (!Files.exists(filePath)) {
            return new HashSet<>();
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            Set<String> notEmpty = new HashSet<>();
            for (String line: lines) {
                String cleaned = line.strip();

                if (cleaned.isBlank()) {
                    continue;
                }

                notEmpty.add(cleaned);
            }
            return notEmpty;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Сохраняет множество в файл, ПОЛНОСТЬЮ перезаписывая его (overwrite).
     * После вызова файл точно равен переданному множеству.
     *
     * @param seen множество номеров, которое надо записать на диск
     */
    public void save(Set<String> seen){
        try {
            Files.write(filePath, seen);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
