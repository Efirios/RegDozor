package org.regdozor.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.regdozor.match.ObligationRisk;
import org.regdozor.profile.Subject;
import org.regdozor.profile.UserProduct;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlertBuilderTest {
    @Test
    @DisplayName("Символы <, >, & из чужих данных экранируются, наша разметка сохраняется")
    void escapingSomeoneElseText() {
        List<UserProduct> userProducts = new ArrayList<>();
        userProducts.add(new UserProduct("Майка <b>&</b> шорты", "6110", "14.39.10", "одежда"));
        List<String> stages = new ArrayList<>();
        List<ObligationRisk> obligationRisks = new ArrayList<>();
        Subject subject = Subject.IP;
        String url = "";

        AlertBuilder alertBuilder = new AlertBuilder();
        String result = alertBuilder.glueMessage(userProducts, stages, obligationRisks, subject, url);

        assertTrue(result.contains("&lt;b&gt;"));
        assertFalse(result.contains("&amp;amp;"));
        assertTrue(result.contains("<u><b>"));
    }
}
