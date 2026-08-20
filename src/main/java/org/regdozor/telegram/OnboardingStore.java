package org.regdozor.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Хранилище НЕЗАКОНЧЕННЫХ разговоров: на каком шаге анкета у каждого собеседника и что он уже ответил.
 *
 * Близнец {@code ProfileStore} по устройству, но с другим смыслом и одним лишним методом.
 * Разница по смыслу: профиль — это РЕЗУЛЬТАТ анкеты, он живёт долго; состояние разговора — ЧЕРНОВИК,
 * он существует, только пока анкета не дозаполнена, и в конце удаляется.
 *
 * ⚠️ Почему в файлах, а не в памяти процесса. Бот крутится на домашней машине: перезагрузки, спящий
 * режим, обновления. Держи мы разговоры в {@code Map} внутри класса — при любом перезапуске все
 * незаконченные анкеты исчезли бы, и человек, дошедший до третьего товара, начал бы с нуля (и, скорее
 * всего, бросил). Анкета длинная: форма, группа, потом по три сообщения на каждый товар.
 *
 * ⚠️ Запись в два хода (черновик рядом → атомарное перемещение) — по той же причине, что в
 * {@code ProfileStore}, и здесь она даже важнее: анкету заполняют долго и вручную.
 *
 * ⚠️ Отсутствие файла = «разговор не идёт». Поэтому чтение отдаёт пустой {@link Optional}, а удаление
 * не жалуется на уже удалённое. Отдельного состояния «анкета завершена» не существует: закончили —
 * записали профиль и стёрли файл.
 */
public class OnboardingStore {
    private final ObjectMapper mapper;
    private static final Path ONBOARDING_DIR = Path.of("data", "onboarding");

    public OnboardingStore(ObjectMapper mapper) {
        if (mapper == null){
            throw new IllegalArgumentException("mapper не может быть null");
        }
        this.mapper = mapper;
    }

    private Path getFilePath(String fileName) {
        return ONBOARDING_DIR.resolve(fileName);
    }

    public Optional<OnboardingState> load(String chatId) {
        Path path = getFilePath(chatId + ".json");

        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            OnboardingState state = mapper.readValue(json, OnboardingState.class);
            return  Optional.ofNullable(state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(String chatId, OnboardingState state) {
        Path path = getFilePath(chatId + ".json");
        Path pathTmp = getFilePath(chatId + ".json.tmp");

        try {
            Files.createDirectories(ONBOARDING_DIR);
            Files.writeString(pathTmp, mapper.writeValueAsString(state), StandardCharsets.UTF_8);
            Files.move(pathTmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Убирает черновик разговора — анкета завершена (или начинается заново).
     *
     * ⚠️ {@code deleteIfExists}, а НЕ {@code delete}: второй бросает исключение, когда файла нет.
     * А здесь его отсутствие ЗАКОННО — анкету могли завершить дважды подряд, состояние могли уже
     * стереть, человек мог начать сначала. Это не поломка, падать не за что. Та же логика, по которой
     * чтение отдаёт пустую коробку вместо исключения. Проверено: повторный вызов проходит молча.
     *
     * 🚨 ПОРЯДОК ВЫЗОВА В ДИАЛОГЕ: сперва {@code profileStore.save(...)}, и только ПОТОМ это удаление.
     * Поменяй местами — и при сбое записи профиля человек останется И БЕЗ ПРОФИЛЯ, И БЕЗ АНКЕТЫ:
     * заполнял пятнадцать шагов, а всё исчезло. Тот же принцип, что в мониторах ленты (отправка ДО
     * {@code save(seen)}): сначала делаем работу, потом помечаем её сделанной.
     *
     * @param chatId идентификатор чата собеседника
     * @throws RuntimeException если файл есть, но удалить не вышло (нет прав, занят другим процессом).
     *                          ⚠️ Не путать со случаем «файла нет» — тот метод обрабатывает сам и молча
     */
    public void delete(String chatId) {
        Path path = getFilePath(chatId + ".json");

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
