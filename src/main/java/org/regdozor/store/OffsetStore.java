package org.regdozor.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "Память" на ОДНО число — offset getUpdates (граница «обработано до сюда»).
 * Аналог SeenStore, но хранит один long, а не множество: load() -> число, save(long) -> файл.
 * Имя файла (внутри data/) задаётся в конструкторе.
 */
public class OffsetStore {
    /** Путь к файлу-хранилищу. Один и тот же на весь срок жизни объекта -> final. */
    private final Path filePath;

    /** @param fileName имя файла внутри папки data/ (напр. "tg-offset.txt") */
    public OffsetStore(String fileName) {
        filePath = Path.of("data", fileName);
    }

    /**
     * Читает сохранённый offset из файла.
     * Если файла ещё нет (самый первый запуск) — возвращает 0 (в Telegram это «отдай все накопившиеся»).
     *
     * @return последний сохранённый offset (или 0, если файла ещё нет)
     */
    public long load(){
        if (!Files.exists(filePath)) {
            return 0;
        }

        try {
            String lines = Files.readString(filePath);
            return Long.parseLong(lines.strip());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Сохраняет offset в файл, ПОЛНОСТЬЮ перезаписывая его (overwrite).
     *
     * @param offset число, которое надо запомнить между запусками
     */
    public void save(long offset){
        try {
            Files.writeString(filePath, Long.toString(offset));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
