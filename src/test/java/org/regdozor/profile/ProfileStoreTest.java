package org.regdozor.profile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
