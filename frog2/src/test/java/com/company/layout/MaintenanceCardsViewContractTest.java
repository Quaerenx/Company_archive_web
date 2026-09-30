package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceCardsViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void globalCardsHideTheirEmptyStateWhenAllCustomersArePersonal()
            throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("maintenance/maintenance_cards.jsp"));
        String guard = "<c:if test=\"${globalMaintenanceCustomerCount gt 0 or personalMaintenanceAssignedCount eq 0}\">";
        int guardStart = page.indexOf(guard);
        int globalStart = page.indexOf("data-global-section=\"maintenance-customers\"");
        int emptyState = page.indexOf("등록된 고객사 정보가 없습니다.");
        int guardEnd = page.indexOf("</c:if>", globalStart);

        assertTrue(guardStart >= 0 && globalStart > guardStart);
        assertTrue(emptyState > globalStart && guardEnd > emptyState);
        assertEquals(1, occurrences(page, guard));
        assertTrue(page.contains("<strong>담당 고객사가 없습니다.</strong>"));
        assertTrue(page.contains("personalMaintenanceAssignedCount eq 0"));
        assertTrue(page.contains("선택한 조건에 해당하는 담당 고객사가 없습니다."));
        assertTrue(page.contains("선택한 조건에 해당하는 고객사가 없습니다."));
    }

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
        assertTrue(card.contains("maintenance-registration-status"));
        assertFalse(card.contains("maintenance-registration-check"));
        assertFalse(card.contains("등록 완료"));
        assertTrue(card.contains("eq '분기'"));
        assertTrue(!card.contains("? '월별' :"));
        assertTrue(styles.contains(
                ".maintenance-management .maintenance-frequency"));
        assertTrue(styles.contains("font-size: var(--font-size-xs)"));
        assertTrue(styles.contains("color: var(--color-text-muted)"));
        assertFalse(styles.contains(
                ".customer-card[data-current-month-registered=\"true\"]"));
        assertFalse(styles.contains("box-shadow: inset 3px 0 0 var(--color-success)"));
        assertTrue(styles.contains(".maintenance-registration-status--registered"));
        assertTrue(styles.contains("background: var(--color-success-bg)"));
        assertTrue(styles.contains("color: var(--color-success-text)"));
        assertTrue(styles.contains(
                ".maintenance-management .inspector-section {\n"
                        + "            flex-direction: column;"));
        assertTrue(styles.contains("min-width: 0;"));
        assertTrue(styles.contains("width: 100%;"));
    }

    @Test
    void selectedMonthBadgesDistinguishRecordRegistrationFromOffCycleCustomers()
            throws Exception {
        String page = Files.readString(WEBAPP.resolve("maintenance/maintenance_cards.jsp"));
        String card = Files.readString(WEBAPP.resolve("WEB-INF/tags/maintenanceCustomerCard.tag"));

        assertEquals(2, occurrences(page,
                "due=\"${maintenanceDueCustomers[customer.customerName]}\""));
        assertEquals(2, occurrences(page, "monthLabel=\"${maintenanceMonthLabel}\""));
        int registered = card.indexOf("<c:when test=\"${registered}\">");
        int due = card.indexOf("<c:when test=\"${due}\">");
        assertTrue(registered >= 0 && due > registered);
        assertTrue(card.contains("value=\"이력 등록\""));
        assertTrue(card.contains("value=\"이력 미등록\""));
        assertTrue(card.contains("value=\"점검 대상 아님\""));
        assertTrue(card.contains("<c:out value=\"${monthLabel}\" />"));
        assertTrue(card.contains("data-maintenance-registration-status="));
        assertFalse(card.contains("이번 달"));
        assertFalse(card.contains("완료"));
        for (String field : new String[] {"customerName", "dbName", "verticaVersion", "mode", "nodes"}) {
            assertTrue(card.contains("<c:out value=\"${customer." + field + "}\" />"), field);
        }
        assertTrue(card.contains("customer-card-arrow\" aria-hidden=\"true\""));
        assertTrue(card.contains("<dl class=\"customer-info\">"));
    }

    @Test
    void monthAndRegistrationFiltersUseNativeGetNavigationAndPreserveEachOther()
            throws Exception {
        String page = Files.readString(WEBAPP.resolve("maintenance/maintenance_cards.jsp"));
        assertTrue(page.contains("method=\"get\""));
        assertTrue(page.contains("type=\"month\""));
        assertTrue(page.contains("min=\"1900-01\""));
        assertTrue(page.contains("max=\"2100-12\""));
        assertTrue(page.contains("name=\"maintenanceMonth\""));
        assertTrue(page.contains("name=\"registrationStatus\" value=\"<c:out value='${registrationStatus}' />\""));
        assertTrue(page.contains("items=\"all,registered,unregistered\""));
        assertTrue(page.contains("<c:param name=\"maintenanceMonth\" value=\"${maintenanceMonthParam}\" />"));
        assertTrue(page.contains("<c:param name=\"registrationStatus\" value=\"${statusOption}\" />"));
        assertTrue(page.contains("href=\"<c:out value='${maintenanceStatusUrl}' />\""));
        assertTrue(page.contains("aria-current=\"${registrationStatus eq statusOption ? 'page' : 'false'}\""));
        assertTrue(page.contains("aria-label=\"선택 월 이력 등록 상태\""));
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
