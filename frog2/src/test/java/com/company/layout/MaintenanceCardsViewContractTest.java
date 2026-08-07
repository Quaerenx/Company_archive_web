package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceCardsViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void customerCardsShowTheConfiguredMaintenanceFrequency() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("maintenance/maintenance_cards.jsp"));
        String styles = Files.readString(
                WEBAPP.resolve("resources/css/pages/maintenance_cards.css"));

        assertEquals(2, occurrences(page, "class=\"maintenance-frequency\""));
        assertTrue(page.contains("maintenanceFrequencyLabels[customer.customerName]"));
        assertTrue(page.contains("? '월별' :"));
        assertTrue(styles.contains(
                ".maintenance-management .maintenance-frequency"));
        assertTrue(styles.contains("font-size: var(--font-size-xs)"));
        assertTrue(styles.contains("color: var(--color-text-muted)"));
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
