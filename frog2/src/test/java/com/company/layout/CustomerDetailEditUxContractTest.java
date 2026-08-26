package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerDetailEditUxContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void longFormProvidesSemanticSectionNavigationAndErrorSummary() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("customers/customers_detail_edit.jsp"));

        assertTrue(page.contains("page-customer-detail-edit"));
        assertTrue(page.contains("aria-label=\"상세정보 수정 섹션\""));
        assertTrue(page.contains("href=\"#customerDetailMeta\""));
        assertTrue(page.contains("href=\"#customerDetailVertica\""));
        assertTrue(page.contains("href=\"#customerDetailEnvironment\""));
        assertTrue(page.contains("href=\"#customerDetailSolutions\""));
        assertTrue(page.contains("href=\"#customerDetailOther\""));
        assertTrue(page.contains("<section id=\"customerDetailMeta\""));
        assertTrue(page.contains("aria-labelledby=\"customerDetailMetaTitle\""));
        assertTrue(page.contains("id=\"customerDetailErrorSummary\""));
        assertTrue(page.contains("customer-detail-form-actions"));
    }

    @Test
    void formWarnsAboutUnsavedChangesWithoutChangingItsServerContract() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("customers/customers_detail_edit.jsp"));
        String script = Files.readString(
                WEBAPP.resolve("resources/js/pages/customer_detail_edit.js"));

        assertTrue(page.contains("name=\"action\" value=\"saveDetail\""));
        assertTrue(page.contains("method=\"post\""));
        assertTrue(script.contains("form.addEventListener('input', markDirty)"));
        assertTrue(script.contains("form.addEventListener('change', markDirty)"));
        assertTrue(script.contains("window.addEventListener('beforeunload'"));
        assertTrue(script.contains("isSubmitting = true"));
    }
}
