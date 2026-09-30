package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PersonalSectionsViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void personalSurfacesPrecedeFullContentAndKeepUniqueElementIds() throws Exception {
        for (List<String> specification : List.of(
                List.of("dashboard.jsp", "dashboard-maintenance", "나의 정기점검"),
                List.of("customers/customers_list.jsp", "customers", "나의 고객사"),
                List.of("maintenance/maintenance_cards.jsp", "maintenance-customers",
                        "나의 정기점검 고객사"))) {
            String page = read(specification.get(0));
            String personalMarker = "data-personal-section=\"" + specification.get(1) + "\"";
            String globalMarker = "data-global-section=\"" + specification.get(1) + "\"";
            int personalStart = page.indexOf(personalMarker);
            int globalStart = page.indexOf(globalMarker);
            assertTrue(personalStart >= 0, specification.get(0));
            assertTrue(globalStart > personalStart, specification.get(0));

            String personal = personalSection(page, specification.get(1));
            assertTrue(personal.contains("ui-work-surface"), specification.get(0));
            assertTrue(personal.contains(specification.get(2)), specification.get(0));
            assertTrue(personal.contains("<strong>담당 고객사가 없습니다.</strong>"),
                    specification.get(0));

            var ids = new HashSet<String>();
            var matcher = Pattern.compile("\\bid=\"([^\"]+)\"").matcher(page);
            while (matcher.find()) {
                assertTrue(ids.add(matcher.group(1)),
                        specification.get(0) + ": duplicate id " + matcher.group(1));
            }
        }
    }

    @Test
    void dashboardDistinguishesUnassignedFromNoDueCustomersInTheSelectedMonth()
            throws Exception {
        String page = read("dashboard.jsp");
        String personal = personalSection(page, "dashboard-maintenance");
        assertTrue(personal.contains("personalMaintenanceAssignedCount eq 0"));
        assertTrue(personal.contains("empty personalMaintenanceCustomers"));
        assertTrue(personal.contains("선택한 달의 정기점검 대상 고객사가 없습니다."));
        assertTrue(personal.contains("maintenanceMonthLabel"));
        assertFalse(personal.contains("maintenanceMonthTabs"));
        assertFalse(personal.contains("toggleMaintenanceBoardBtn"));
        assertEquals(1, occurrences(page, "id=\"toggleMaintenanceBoardBtn\""));
        assertEquals(1, occurrences(page, "id=\"maintenanceMonthBoardBody\""));
        assertTrue(page.contains("customers=\"${group.customers}\""));
        assertTrue(personal.contains("customers=\"${personalMaintenanceCustomers}\""));
    }

    @Test
    void personalCustomerTableSharesRowsWithoutDuplicatingFullListControls()
            throws Exception {
        String page = read("customers/customers_list.jsp");
        String personal = personalSection(page, "customers");
        assertTrue(personal.contains("customer-list-panel"));
        assertTrue(personal.contains("data-ui-return-list-key=\"personal-customers\""));
        assertTrue(personal.contains("customers=\"${personalCustomers}\""));
        assertTrue(page.contains("customers=\"${customerList}\""));
        assertEquals(2, occurrences(page, "<t:customerListRows"));
        assertEquals(8, occurrences(personal, "scope=\"col\""));
        assertTrue(personal.contains("<caption class=\"sr-only\">나의 고객사 정보 목록</caption>"));
        for (String globalControl : List.of(
                "customer-search-form", "search-input", "clear-search", "js-customer-sort",
                "customer-table-body", "<t:tableFooter")) {
            assertFalse(personal.contains(globalControl), globalControl);
        }
    }

    @Test
    void personalAndFullMaintenanceCardsUseTheSameCardRenderer() throws Exception {
        String page = read("maintenance/maintenance_cards.jsp");
        String personal = personalSection(page, "maintenance-customers");
        assertTrue(personal.contains("items=\"${personalMaintenanceCustomers}\""));
        assertTrue(page.contains("items=\"${inspectorCustomers}\""));
        assertTrue(personal.contains("personalMaintenanceAssignedCount eq 0"));
        assertTrue(personal.contains("선택한 조건에 해당하는 담당 고객사가 없습니다."));
        assertEquals(2, occurrences(page, "<t:maintenanceCustomerCard"));
        assertEquals(1, occurrences(page,
                "<c:forEach var=\"entry\" items=\"${inspectorCustomers}\">"));
    }

    private static String personalSection(String page, String key) {
        int marker = page.indexOf("data-personal-section=\"" + key + "\"");
        assertTrue(marker >= 0, key);
        int start = page.lastIndexOf("<section", marker);
        int end = page.indexOf("</section>", marker);
        assertTrue(start >= 0 && end > marker, key);
        return page.substring(start, end + "</section>".length());
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(target, index)) >= 0;
                index += target.length()) {
            count++;
        }
        return count;
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
