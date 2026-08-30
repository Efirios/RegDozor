package org.regdozor.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Помнит между запусками, что уже видели.
 *
 * «Память» приложения: множество строк в текстовом файле (по одной строке на элемент). Обобщённое хранилище — что именно за строки, решает вызывающий:
 * eoNumber-ы документов pravo (файл seen-eonumbers.txt), ссылки на выпуски ЦРПТ (seen-releases.txt) и т.п.
 *
 * Благодаря ему приложение при каждом запуске понимает, что уже видело, а что новое.
 * Имя файла (внутри папки data/) задаётся в конструкторе.
 * Роли методов зеркальные: load() читает диск -> память, save() пишет память -> диск.
 */
public class SeenStore {
    /** Путь к файлу-хранилищу. Один и тот же на весь срок жизни объекта -> final. */
    private final Path filePath;

    /** @param fileName имя файла внутри папки data/ (напр. "seen-releases.txt") */
    public SeenStore(String fileName, Path dataDir) {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName не может быть null");
        }

        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir не может быть null");
        }
        this.filePath = dataDir.resolve(fileName);
    }

    public SeenStore(String fileName) {
        this(fileName, Path.of("data"));
    }

    /**
     * Читает множество строк из файла.
     * Если файла ещё нет (самый первый запуск) — возвращает пустое множество.
     * Пустые строки и пробелы по краям отбрасываются.
     *
     * @return множество строк, известных из прошлых запусков
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
