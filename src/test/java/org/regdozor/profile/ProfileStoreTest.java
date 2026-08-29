package org.regdozor.profile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileStoreTest {
    ObjectMapper objectMapper = new ObjectMapper();
    ProfileStore profileStore;
    List<UserProduct> userProducts = new ArrayList<>();
    Profile profile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp(){
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        userProducts.add(new UserProduct("Майка <b>&</b> шорты", "6110", "14.39.10", "одежда"));
        profileStore = new ProfileStore(objectMapper, tempDir);
        profile = new Profile(
                List.of("лёгкая промышленность"),
                List.of("shoes-and-clothes"),
                Subject.IP,
                userProducts);
    }

    @Test
    @DisplayName("Отсутствие профиля — норма: пустой Optional, не null и не исключение")
    void ifThereIsNoProfileThenAnEmptyBox() {
        Optional<Profile> result = profileStore.load("123456");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Сохранённый профиль читается обратно без потерь")
    void theSavedProfileCanBeReadBackWithoutAnyLoss() {
        profileStore.save("123456", profile);
        Optional<Profile> result = profileStore.load("123456");
        assertEquals(profile, result.orElseThrow());
    }

    @Test
    @DisplayName("Профиль сохраняется в <каталог>/<chatId>.json")
    void theProfileIsSavedWhereItNeedsToBe() {
        profileStore.save("123456", profile);
        Path profileFile = tempDir.resolve("123456.json");
        assertTrue(Files.exists(profileFile));
    }

    @Test
    @DisplayName("После сохранения временный файл не остаётся")
    void thereIsNoTemporaryFileLeft() {
        profileStore.save("123456", profile);
        Path tmpFile = tempDir.resolve("123456.json.tmp");
        assertFalse(Files.exists(tmpFile));
    }

    @Test
    @DisplayName("Повторное сохранение заменяет прежний профиль")
    void savingAgainReplacesThePreviousProfile() {
        profileStore.save("123456", profile);
        Profile profile2 = new Profile(
                List.of("лёгкая промышленность"),
                List.of("shoes-and-clothes"),
                Subject.LEGAL,
                userProducts);
        profileStore.save("123456", profile2);
        Optional<Profile> result = profileStore.load("123456");
        assertEquals(profile2, result.orElseThrow());
    }

    @Test
    @DisplayName("Битый json роняет load с исключением, а не выдаёт пустой результат")
    void brokenJsonDropsLoudly() throws IOException {
        Path profileFile = tempDir.resolve("123456.json");
        Files.writeString(profileFile, "не json");
        assertThrows(RuntimeException.class, () -> profileStore.load("123456"));
    }
}
