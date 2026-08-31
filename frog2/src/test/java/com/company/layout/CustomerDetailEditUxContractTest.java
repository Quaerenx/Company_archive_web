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
    }

    @Test
    void noteFieldsProvideAFullHeightEditingArea() throws Exception {
        String styles = Files.readString(
                WEBAPP.resolve("resources/css/pages/customer_detail.css"));

        assertTrue(styles.contains(
                ".customer-detail--edit textarea.customer-detail-edit-control"));
        assertTrue(styles.contains("min-block-size: 176px;"));
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
}
