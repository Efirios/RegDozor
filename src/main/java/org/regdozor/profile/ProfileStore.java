package org.regdozor.profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Хранилище профилей ПО ПОДПИСЧИКАМ: достать профиль конкретного chat_id и надёжно сохранить его.
 *
 * Ключ к мультипользовательности (уровень B). До него профиль был ОДИН на всех и лежал ресурсом внутри
 * JAR — а значит подписавшееся юрлицо получало бы ИП-шные суммы штрафа, заниженные в 30 раз.
 *
 * Файлы живут в {@code data/profiles/<chatId>.json} — там же, где остальное состояние
 * ({@code chat-ids.txt}, {@code seen-*.txt}). Весь каталог {@code data/} в .gitignore, поэтому данные
 * клиентов не утекут в репозиторий сами собой.
 *
 * Отдельный класс, а не правка {@code ProfileLoader}: тот читает РЕСУРС ИЗ JAR — один на всех и
 * неизменяемый, писать в него нельзя физически. Здесь другое место, другой ключ (chat_id) и другое
 * направление (ещё и запись).
 *
 * Чего НЕ делает: не спрашивает человека (это онбординг), не проверяет осмысленность профиля
 * (что коды валидны, что группа есть в obligations.json), не решает релевантность.
 */
public class ProfileStore {
    // «переводчик» json ↔ объекты. Внедряется, а не создаётся здесь: в App он уже настроен
    // (FAIL_ON_UNKNOWN_PROPERTIES=false), и второй экземпляр со своими настройками однажды разойдётся с первым
    private final ObjectMapper mapper;
    // каталог профилей — написан ОДИН раз на весь класс, чтобы чтение и запись не разъехались по опечатке
    private static final Path PROFILES_DIR = Path.of("data", "profiles");

    public ProfileStore(ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper не может быть null");
        }
        this.mapper = mapper;
    }

    /**
     * Составляет адрес файла внутри каталога профилей.
     *
     * ⚠️ Через {@code resolve}, а НЕ склейкой строк: {@code PROFILES_DIR + fileName} дало бы
     * {@code data\profiles123456789.json} — без разделителя, то есть файл лёг бы не туда.
     * {@code resolve} ставит разделитель нужной операционной системы сам.
     */
    private Path getFilePath(String fileName) {
        return PROFILES_DIR.resolve(fileName);
    }

    /**
     * Отдаёт профиль подписчика, если он есть.
     *
     * ⚠️ Пустой {@link Optional} — это НОРМА, а не поломка: человек подписался, но онбординг ещё
     * не проходил. Именно поэтому здесь коробка, а не {@code null} (вызывающий забыл бы проверить)
     * и не исключение (исключениями кричим о поломке источника, а тут ломаться нечему).
     *
     * @param chatId идентификатор чата подписчика
     * @return профиль в коробке; пустая коробка, если файла нет
     * @throws RuntimeException если файл есть, но нечитаем или в нём битый json — падаем ГРОМКО.
     *                          Вернуть пустую коробку было бы хуже: «файл повреждён» и «человек не
     *                          заполнял профиль» стали бы неразличимы, и дозор для него молча замолчал бы
     */
    public Optional<Profile> load(String chatId) {
        Path path = getFilePath(chatId + ".json");

        // Path — это только адрес, про существование он ничего не знает; спрашиваем отдельно у Files
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            // кодировка ЯВНО: без неё взялась бы системная (здесь windows-1251) и кириллица в именах
            // товаров («Майка женская») превратилась бы в кракозябры
            String json = Files.readString(path, StandardCharsets.UTF_8);
            // .class, а не [].class — в файле один объект {}, а не массив []
            Profile profile = mapper.readValue(json, Profile.class);
            // ofNullable, а не of: of на null бросает исключение, а в файле теоретически может лежать «null»
            return Optional.ofNullable(profile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Сохраняет профиль подписчика, не рискуя потерять уже сохранённый.
     *
     * 🚨 ЗАПИСЬ В ДВА ХОДА («во временный, потом переименовать») — НЕ переставлять и не упрощать.
     * Файл перезаписывается ЦЕЛИКОМ, а профиль правится регулярно: продавец добавляет товар, вводит
     * новые коды. Обрыв посреди прямой записи стоил бы не «потеряли новый товар», а ПОТЕРЯЛИ ПРОФИЛЬ
     * ЦЕЛИКОМ — и дозор для этого человека молча перестал бы работать. Переименование же файловая
     * система делает целиком или никак: по целевому адресу всегда лежит либо старый полный профиль,
     * либо новый полный.
     *
     * ⚠️ Временный файл ОБЯЗАН лежать в том же каталоге: перемещение внутри одного тома атомарно,
     * через границу дисков оно превращается в «скопировать и удалить» и теряет весь смысл.
     * ⚠️ {@code ATOMIC_MOVE} — это и есть та самая неделимость; без него система вправе выполнить
     * перемещение копированием, и окно для сбоя вернётся. {@code REPLACE_EXISTING} нужен потому,
     * что при втором и последующих сохранениях целевой файл уже существует (иначе — исключение).
     * Если файловая система атомарного перемещения не умеет, будет {@code AtomicMoveNotSupportedException} —
     * ловить и глушить его НЕ надо: честный отказ лучше молчаливой небезопасной записи.
     *
     * @param chatId  идентификатор чата подписчика
     * @param profile что сохранить
     * @throws RuntimeException если записать или переместить не удалось
     */
    public void save(String chatId, Profile profile) {
        Path path = getFilePath(chatId + ".json");            // куда в итоге должно лечь
        Path pathTmp = getFilePath(chatId + ".json.tmp");     // черновик — рядом, в том же каталоге

        try {
            // каталог может не существовать при самом первом сохранении; существующий метод молча пропустит
            Files.createDirectories(PROFILES_DIR);
            // пишем ЧЕРНОВИК: пока идёт запись, рабочий профиль на месте и цел
            Files.writeString(pathTmp, mapper.writeValueAsString(profile), StandardCharsets.UTF_8);
            // и только теперь одним неделимым движением подменяем рабочий файл черновиком
            Files.move(pathTmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
