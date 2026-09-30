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
        String card = Files.readString(
                WEBAPP.resolve("WEB-INF/tags/maintenanceCustomerCard.tag"));
        String styles = Files.readString(
                WEBAPP.resolve("resources/css/pages/maintenance_cards.css"));

        assertEquals(1, occurrences(card, "class=\"maintenance-frequency\""));
        assertEquals(1, occurrences(card, "class=\"customer-card\""));
        assertEquals(2, occurrences(page, "<t:maintenanceCustomerCard"));
        assertEquals(1, occurrences(page,
                "<c:forEach var=\"entry\" items=\"${inspectorCustomers}\">"));
        assertTrue(page.contains(
                "class=\"inspector-block ui-work-surface ui-work-surface--padded\""));
        assertTrue(page.contains("maintenanceFrequencyLabels[customer.customerName]"));
        assertTrue(page.contains(
                "registered=\"${currentMonthMaintenanceCustomers[customer.customerName]}\""));
        assertTrue(card.contains("data-current-month-registered=\"${registered ? 'true' : 'false'}\""));
        assertTrue(card.contains("fas fa-check-circle"));
        assertTrue(card.contains("maintenance-registration-check"));
        assertTrue(card.contains("aria-label=\"이번 달 등록 완료\""));
        assertTrue(!card.contains("maintenance-registration-status"));
        assertTrue(!card.contains("ui-badge--success"));
        assertTrue(card.contains("eq '분기'"));
        assertTrue(!card.contains("? '월별' :"));
        assertTrue(styles.contains(
                ".maintenance-management .maintenance-frequency"));
        assertTrue(styles.contains("font-size: var(--font-size-xs)"));
        assertTrue(styles.contains("color: var(--color-text-muted)"));
        assertTrue(styles.contains(
                ".customer-card[data-current-month-registered=\"true\"]"));
        assertTrue(styles.contains("border-color: var(--color-success-border)"));
        assertTrue(styles.contains("box-shadow: inset 3px 0 0 var(--color-success)"));
        assertTrue(styles.contains(
                ".maintenance-management .maintenance-registration-check"));
        assertTrue(styles.contains("inset-block-start: var(--space-12)"));
        assertTrue(styles.contains("inset-inline-end: var(--space-12)"));
        assertTrue(styles.contains("position: absolute"));
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
