package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MotionEnhancementContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void navigationAndDialogsUseQuietReversibleMotion() throws Exception {
        String header = read("resources/css/pages/header.css");
        String components = read("resources/css/components.css");
        String uiScript = read("resources/js/ui-system.js");

        assertTrue(header.contains("opacity: 0;"));
        assertTrue(header.contains("pointer-events: none;"));
        assertTrue(header.contains("visibility: hidden;"));
        assertTrue(header.contains(".main-nav .dropdown.open > .dropdown-menu"));
        assertTrue(header.contains("opacity: 1;"));
        assertTrue(header.contains(".main-nav .dropdown:not(.open) > .dropdown-menu"));

        assertTrue(components.contains(".modal.show .modal-content"));
        assertTrue(components.contains("scale(0.985)"));
        assertTrue(components.contains("pointer-events: none;"));
        assertFalse(components.contains("@keyframes slideDown"));
        assertTrue(uiScript.contains("dialog.removeAttribute('inert')"));
        assertTrue(uiScript.contains("dialog.setAttribute('inert', '')"));
    }

    @Test
    void toastAndAsyncCompletionHaveExitAndSuccessStates() throws Exception {
        String styles = read("resources/css/ui-system.css");
        String script = read("resources/js/ui-system.js");

        assertTrue(styles.contains(".ui-toast.is-leaving"));
        assertTrue(styles.contains("@keyframes frog2-ui-toast-out"));
        assertTrue(styles.contains(".ui-button.is-success"));
        assertTrue(styles.contains("content: \"\\2713\""));
        assertTrue(script.contains("function dismissToast(toast)"));
        assertTrue(script.contains("function setButtonSuccess(button, label, duration)"));
        assertTrue(script.contains("setButtonSuccess: setButtonSuccess"));
    }

    @Test
    void customerTabsShareOneSlidingIndicator() throws Exception {
        String page = read("customers/customers_detail.jsp");
        String styles = read("resources/css/pages/customer_detail.css");
        String script = read("resources/js/pages/customer_detail.js");

        assertTrue(page.contains("class=\"tab-indicator\" aria-hidden=\"true\""));
        assertTrue(styles.contains(".customer-detail--view .tab-indicator"));
        assertTrue(styles.contains("inline-size var(--motion-base)"));
        assertTrue(script.contains("function syncTabIndicator()"));
        assertTrue(script.contains("activeTab.offsetWidth"));
        assertTrue(script.contains("activeTab.offsetLeft"));
    }

    @Test
    void uploadQueueShowsOnlyRealLifecycleStates() throws Exception {
        String styles = read("resources/css/pages/upload.css");
        String script = read("resources/js/pages/file_repository_upload.js");

        assertTrue(script.contains("item.dataset.fileState = 'queued'"));
        assertTrue(script.contains("setFileState('uploading', '업로드 중')"));
        assertTrue(script.contains("setFileState('complete', '완료')"));
        assertTrue(script.contains("setFileState('error', '오류')"));
        assertTrue(script.contains("window.Frog2UI.setButtonSuccess"));
        assertTrue(styles.contains("[data-file-state=\"uploading\"]"));
        assertTrue(styles.contains("prefers-reduced-motion: reduce"));
        assertFalse(script.contains("Math.random"));
        assertFalse(script.contains("fakeProgress"));
    }

    @Test
    void disclosureRowsKeepDynamicHeightWithoutAnEndFrameSnap() throws Exception {
        String styles = read("resources/css/ui-system.css");
        String script = read("resources/js/ui-system.js");

        assertTrue(styles.contains("[data-ui-disclosure-content]"));
        assertTrue(styles.contains("grid-template-rows: 0fr;"));
        assertTrue(styles.contains("grid-template-rows: 1fr;"));
        assertTrue(styles.contains("min-block-size: 0;"));
        assertTrue(styles.contains(
                "[data-ui-disclosure-content] > .ui-disclosure-clip"));
        assertTrue(read("maintenance/maintenance_history.jsp").contains(
                "class=\"history-detail-cell ui-disclosure-cell\""));
        assertFalse(read("customer-history/customer_history_list.jsp").contains(
                "data-ui-disclosure-content"));
        String fixedCellRule = ".ui-system .ui-data-table :is(th, td) {";
        String disclosureCellRule = ".ui-system .ui-data-table .ui-disclosure-cell {\n"
                + "    block-size: auto;\n"
                + "    border-block-end: 0;\n"
                + "    padding: 0;";
        assertTrue(styles.contains(disclosureCellRule));
        assertTrue(styles.indexOf(disclosureCellRule) > styles.indexOf(fixedCellRule));
        assertFalse(styles.contains(
                "grid-template-rows var(--motion-base) var(--motion-ease),\n"
                        + "        opacity"));
        assertTrue(script.contains("is-disclosure-expanded"));
        assertTrue(script.contains("event.propertyName === 'grid-template-rows'"));
        assertFalse(script.contains("var targetHeight = expanded ? contentHeight : 0;"));
        assertFalse(script.contains("height: targetHeight + 'px'"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
