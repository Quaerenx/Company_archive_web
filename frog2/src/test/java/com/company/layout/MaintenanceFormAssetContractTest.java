package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceFormAssetContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path PAGE_SCRIPTS = WEBAPP.resolve("resources/js/pages");

    @Test
    void addAndEditPagesUseOneExplicitFormContract() throws Exception {
        String add = readWebapp("maintenance/maintenance_add.jsp");
        String edit = readWebapp("maintenance/maintenance_edit.jsp");

        assertTrue(add.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(edit.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(add.contains("data-maintenance-form-mode=\"add\""));
        assertTrue(edit.contains("data-maintenance-form-mode=\"edit\""));
        assertEquals(1, countOccurrences(add, "id=\"maintenanceForm\""));
        assertEquals(1, countOccurrences(edit, "id=\"maintenanceForm\""));
        assertTrue(edit.contains("id=\"deleteFormHeader\""));
        assertTrue(edit.contains("id=\"current_customer_value\""));
        assertTrue(edit.contains("id=\"current_inspector_value\""));
    }

    @Test
    void sharedScriptPreservesModeSpecificBehaviorAndFailsClosed() throws Exception {
        String script = Files.readString(PAGE_SCRIPTS.resolve("maintenance_form.js"));

        assertTrue(script.contains("(function()"));
        assertTrue(script.contains("getElementById('maintenanceForm')"));
        assertFalse(script.contains("querySelector('form')"));
        assertEquals(1, countOccurrences(
                script, "/customers?action=getCustomersForMaintenance"));
        assertTrue(script.contains("if (!response.ok)"));
        assertTrue(script.contains("function ensureOption("));
        assertTrue(script.contains("current_customer_value"));
        assertTrue(script.contains("current_inspector_value"));
        assertTrue(script.contains("getFullYear()"));
        assertTrue(script.contains("getMonth() + 1"));
        assertTrue(script.contains("getDate()"));
        assertFalse(script.contains("toISOString()"));
        assertTrue(script.contains("optionsUnavailable = true"));
        assertTrue(script.contains("고객사 및 점검자 정보를 불러오지 못했습니다."));
        assertFalse(script.contains("직접 입력"));
        assertTrue(script.contains("정말 삭제하시겠습니까?"));
    }

    @Test
    void obsoletePageScriptsAreRemoved() {
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_add.js")));
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_edit.js")));
    }

    @Test
    void historyScriptSkipsChartRenderingWhenVendorIsUnavailable() throws Exception {
        String history = Files.readString(PAGE_SCRIPTS.resolve("maintenance_history.js"));

        assertTrue(history.contains("typeof window.Chart !== 'function'"));
        assertTrue(history.contains("new window.Chart("));
        assertTrue(history.contains("prefers-reduced-motion: reduce"));
        assertTrue(history.contains("typeof item.animate === 'function'"));
        assertFalse(history.contains("item.addEventListener('click'"));
        assertTrue(readWebapp("maintenance/maintenance_history.jsp")
                .contains("<a class=\"history-item\""));
    }

    private static String readWebapp(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static int countOccurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
