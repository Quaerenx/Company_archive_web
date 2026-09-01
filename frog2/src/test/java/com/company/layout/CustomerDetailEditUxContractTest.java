package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerDetailEditUxContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void editPageMirrorsTheDetailHierarchyForEveryEnvironment() throws Exception {
        String page = readFormMarkup();

        assertTrue(page.contains("page-customer-detail-edit"));
        assertTrue(page.contains("/resources/css/pages/customer_detail.css"));
        assertTrue(page.contains("aria-label=\"수정할 고객사 환경\""));
        assertTrue(page.contains("role=\"tab\""));
        assertTrue(page.contains("role=\"tabpanel\""));
        assertTrue(page.contains("data-customer-detail-form"));
        assertTrue(page.contains("name=\"env\" value=\"<c:out value='${environment.value}' />\""));
        assertTrue(page.contains("detail-section detail-section--summary"));
        assertTrue(page.contains("핵심 정보"));
        assertTrue(page.contains("기본·담당자 정보"));
        assertTrue(page.contains("Vertica 운영 설정"));
        assertTrue(page.contains("인프라·네트워크"));
        assertTrue(page.contains("연계 솔루션"));
        assertTrue(page.contains("기타 정보"));
        assertTrue(page.contains("data-customer-detail-error-summary"));
        assertTrue(page.contains("customer-detail-form-actions"));
        assertTrue(page.contains("data-detail-section-count"));
        assertTrue(page.contains("idPrefix=\"${editIdPrefix}\""));
        assertTrue(page.contains(
                "options=\"온프레미스:온프레미스|클라우드:클라우드|하이브리드:하이브리드\""));
        assertTrue(page.contains("options=\"ENT:ENT|EON:EON\""));
        assertTrue(page.contains("options=\"Y:사용|N:미사용\""));
    }

    @Test
    void eachEnvironmentKeepsAnIndependentSaveContractAndDraftWarning() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("customers/customers_detail_edit.jsp"));
        String script = Files.readString(
                WEBAPP.resolve("resources/js/pages/customer_detail_edit.js"));

        assertTrue(page.contains("name=\"action\" value=\"saveDetail\""));
        assertTrue(page.contains("method=\"post\""));
        assertTrue(page.contains("data-environment=\"<c:out value='${environment.value}' />\""));
        assertTrue(script.contains("var dirtyForms = new Set()"));
        assertTrue(script.contains("form.addEventListener('input'"));
        assertTrue(script.contains("form.addEventListener('change'"));
        assertTrue(script.contains("otherDirtyCount > 0"));
        assertTrue(script.contains("window.addEventListener('beforeunload'"));
        assertTrue(script.contains("isSubmitting = true"));
        assertTrue(script.contains("setActiveEnvironment"));
        assertTrue(script.contains("window.addEventListener('popstate'"));
        assertTrue(script.contains("input.name === 'createDate'"));
        assertTrue(script.contains("input.name === 'installDate'"));
    }

    @Test
    void sharedEditFieldTagGeneratesUniqueAccessibleControls() throws Exception {
        String tag = Files.readString(
                WEBAPP.resolve("WEB-INF/tags/customerDetailEditField.tag"));

        assertTrue(tag.contains("value=\"${idPrefix}-${name}\""));
        assertTrue(tag.contains("<label class=\"detail-label\" for="));
        assertTrue(tag.contains("name=\"<c:out value='${name}' />\""));
        assertTrue(tag.contains("data-customer-detail-field"));
        assertTrue(tag.contains("readonly aria-readonly=\"true\""));
        assertTrue(tag.contains("disabled aria-disabled=\"true\""));
        assertTrue(tag.contains(
                "customer-detail-edit-control--blocked"));
        assertTrue(tag.contains("data-conditional-field-mirror="));
    }

    @Test
    void noteFieldsProvideAFullHeightEditingArea() throws Exception {
        String styles = Files.readString(
                WEBAPP.resolve("resources/css/pages/customer_detail.css"));

        assertTrue(styles.contains(
                ".ui-system .customer-detail--edit\n"
                        + "        textarea.customer-detail-edit-control.note-textarea"));
        assertTrue(styles.contains("min-block-size: 192px;"));
    }

    @Test
    void conditionallyBlockedControlsUseAnObviousMutedSurface() throws Exception {
        String styles = Files.readString(
                WEBAPP.resolve("resources/css/pages/customer_detail.css"));
        String uiSystem = Files.readString(
                WEBAPP.resolve("resources/css/ui-system.css"));

        assertTrue(styles.contains(
                ".detail-item.is-conditionally-disabled .detail-label"));
        assertTrue(styles.contains(
                ".detail-item.is-conditionally-disabled"));
        assertTrue(styles.contains(
                ".customer-detail-edit-control:disabled"));
        assertTrue(styles.contains(
                ".customer-detail-edit-control--blocked"));
        assertTrue(styles.contains(
                "background: var(--color-surface-muted);"));
        assertTrue(styles.contains(
                "border-color: var(--color-surface-edge);"));
        assertTrue(styles.contains(
                "color: var(--color-text-disabled);"));
        assertTrue(styles.contains("opacity: 1;"));
        assertTrue(uiSystem.contains(
                ".ui-system form.ui-form :where(\n"
                        + "    input:not([type=\"hidden\"]):not([type=\"checkbox\"]):not([type=\"radio\"]):not([type=\"file\"]),"));
        assertTrue(uiSystem.indexOf(
                ".ui-system form.ui-form :is(input, select, textarea):disabled")
                > uiSystem.indexOf(".ui-system form.ui-form :where("));
    }

    @Test
    void editFormKeepsTheRequestedTwoColumnFieldOrder() throws Exception {
        String summary = Files.readString(
                WEBAPP.resolve("customers/_detail_edit_summary.jspf"));
        String meta = Files.readString(
                WEBAPP.resolve("customers/_detail_edit_meta.jspf"));
        String vertica = Files.readString(
                WEBAPP.resolve("customers/_detail_edit_vertica.jspf"));
        String environment = Files.readString(
                WEBAPP.resolve("customers/_detail_edit_environment.jspf"));

        assertAppearsInOrder(summary,
                "name=\"dbName\"",
                "name=\"osInfo\"",
                "name=\"verticaVersion\"",
                "name=\"said\"",
                "name=\"dbMode\"",
                "name=\"mainManager\"",
                "name=\"nodeCount\"",
                "name=\"subManager\"",
                "name=\"licenseInfo\"");
        assertAppearsInOrder(meta,
                "name=\"customerName\"",
                "name=\"systemName\"",
                "name=\"customerManager\"",
                "name=\"introductionYear\"",
                "name=\"siCompany\"",
                "name=\"installDate\"",
                "name=\"siManager\"",
                "name=\"createDate\"",
                "name=\"creator\"");
        assertAppearsInOrder(vertica,
                "name=\"verticaAdmin\"",
                "name=\"mcYn\"",
                "name=\"customResourcePoolYn\"",
                "name=\"mcVersion\"",
                "name=\"subclusterYn\"",
                "name=\"mcHost\"",
                "name=\"backupYn\"",
                "name=\"mcAdmin\"",
                "name=\"backupNote\"");
        assertAppearsInOrder(environment,
                "name=\"memoryInfo\"",
                "name=\"infraType\"",
                "name=\"swapMemory\"",
                "name=\"dataArea\"",
                "name=\"cpuSocket\"",
                "name=\"catalogArea\"",
                "name=\"cpuCore\"",
                "name=\"depotArea\"",
                "name=\"hyperThreading\"",
                "name=\"objectArea\"",
                "name=\"publicNetwork\"",
                "name=\"privateNetwork\"",
                "name=\"storageNetwork\"");
        assertTrue(!environment.contains("name=\"publicYn\""));
        assertTrue(!environment.contains("name=\"privateYn\""));
        assertTrue(!environment.contains("name=\"storageYn\""));
        assertTrue(environment.contains("columnStart=\"${true}\""));
        assertTrue(environment.contains("disabled=\"${eonFieldsDisabled}\""));
        assertTrue(vertica.contains("disabled=\"${mcFieldsDisabled}\""));
        assertTrue(!meta.contains("name=\"subManager\""));
    }

    private static String readFormMarkup() throws Exception {
        StringBuilder source = new StringBuilder(Files.readString(
                WEBAPP.resolve("customers/customers_detail_edit.jsp")));
        for (String section : new String[] {
                "summary", "meta", "vertica", "environment", "solutions", "other"}) {
            source.append(Files.readString(WEBAPP.resolve(
                    "customers/_detail_edit_" + section + ".jspf")));
        }
        return source.toString();
    }

    private static void assertAppearsInOrder(String value, String... tokens) {
        int offset = 0;
        for (String token : tokens) {
            int index = value.indexOf(token, offset);
            assertTrue(index >= 0, "Missing or out-of-order token: " + token);
            offset = index + token.length();
        }
    }
}
