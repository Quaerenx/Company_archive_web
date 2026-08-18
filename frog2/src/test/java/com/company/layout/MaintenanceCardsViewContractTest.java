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

        assertEquals(1, occurrences(page, "class=\"maintenance-frequency\""));
        assertEquals(1, occurrences(page, "class=\"customer-card\""));
        assertEquals(1, occurrences(page,
                "<c:forEach var=\"entry\" items=\"${inspectorCustomers}\">"));
        assertTrue(page.contains(
                "class=\"inspector-block ui-work-surface ui-work-surface--padded\""));
        assertTrue(page.contains("maintenanceFrequencyLabels[customer.customerName]"));
        assertTrue(page.contains("eq '분기'"));
        assertTrue(!page.contains("? '월별' :"));
        assertTrue(styles.contains(
                ".maintenance-management .maintenance-frequency"));
        assertTrue(styles.contains("font-size: var(--font-size-xs)"));
        assertTrue(styles.contains("color: var(--color-text-muted)"));
        assertTrue(styles.contains(
                ".maintenance-management .inspector-section {\n"
                        + "            flex-direction: column;"));
        assertTrue(styles.contains("min-width: 0;"));
        assertTrue(styles.contains("width: 100%;"));
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
