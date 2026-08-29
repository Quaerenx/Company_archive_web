package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerDetailViewStructureTest {
    @Test
    void allEnvironmentsUseTheSameEncodedDetailFragment() throws Exception {
        Path customers = Path.of("src/main/webapp/customers");
        String page = Files.readString(customers.resolve("customers_detail.jsp"));
        String fragment = Files.readString(customers.resolve("_detail_sections.jspf"));

        assertEquals(3, occurrences(page, "<%@ include file=\"/customers/_detail_sections.jspf\" %>"));
        assertFalse(page.contains("customerDetail.systemName"));
        assertTrue(fragment.contains(
                "<t:detailField label=\"시스템명\" value=\""
                        + "$" + "{detail.systemName}\" hideWhenEmpty=\"${true}\" />"));
        assertTrue(fragment.contains(
                "<fmt:formatDate var=\"detailEosDate\" value=\""
                        + "$" + "{detail.eosDate}\""));
        assertEquals(49, occurrences(fragment, "<t:detailField "));
        assertEquals(5, occurrences(
                fragment,
                "class=\"detail-section detail-section--collapsible\" data-detail-section open"));
        assertTrue(fragment.contains("핵심 정보"));
        assertTrue(fragment.contains("기본·담당자 정보"));
        assertTrue(fragment.contains("인프라·네트워크"));
    }

    @Test
    void activeCustomerOwnsActionsAndExplicitEnvironmentOwnsInitialTab() throws Exception {
        Path webapp = Path.of("src/main/webapp");
        String page = Files.readString(webapp.resolve("customers/customers_detail.jsp"));
        String script = Files.readString(
                webapp.resolve("resources/js/pages/customer_detail.js"));

        assertTrue(page.contains("<c:if test=\"" + "$" + "{not empty customer}\">"));
        assertTrue(script.contains(
                "var envParam = (params.get('env') || '').trim().toLowerCase()"));
        assertTrue(script.contains("var hasRequestedEnvironment = envParam === 'prod'"));
        assertTrue(script.contains("if (!hasRequestedEnvironment && prodEmpty)"));
        assertTrue(script.contains("window.history[replace ? 'replaceState' : 'pushState']"));
        assertTrue(script.contains("window.addEventListener('popstate'"));
        assertTrue(script.contains("url.searchParams.set('env', environment)"));
    }

    @Test
    void readOnlyDetailUsesAResponsiveTableLikeHierarchy() throws Exception {
        Path webapp = Path.of("src/main/webapp");
        String page = Files.readString(
                webapp.resolve("customers/customers_detail.jsp"));
        String styles = Files.readString(
                webapp.resolve("resources/css/pages/customer_detail.css"));
        String fieldTag = Files.readString(
                webapp.resolve("WEB-INF/tags/detailField.tag"));
        String fields = Files.readString(
                webapp.resolve("customers/_detail_sections.jspf"));

        assertTrue(page.contains("customer-detail--view"));
        assertTrue(styles.contains(
                "grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(styles.contains(
                "grid-template-columns: minmax(152px, 160px) minmax(0, 1fr);"));
        assertTrue(styles.contains("white-space: nowrap;"));
        assertFalse(styles.contains("minmax(96px, 112px)"));
        assertTrue(fields.contains("label=\"사용자 정의 리소스 풀\""));
        assertTrue(styles.contains("details[open] > .detail-section-title"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
        assertTrue(fieldTag.contains("detail-value--empty\">미등록</span>"));
        assertTrue(fieldTag.contains("name=\"hideWhenEmpty\""));
        assertTrue(fieldTag.contains("detail-item--empty"));
        assertTrue(fieldTag.contains("<c:out value=\"${value}\" />"));
        assertTrue(fields.contains("data-detail-section-count"));
        assertTrue(fields.contains("${detail.licenseDisplay}"));
        assertTrue(fields.contains("${detail.nodeCountDisplay}"));
    }

    @Test
    void collapsibleInspectorSectionsShareOneAlignmentAndQuietHierarchy() throws Exception {
        String styles = Files.readString(Path.of(
                "src/main/webapp/resources/css/pages/customer_detail.css"));

        String section = cssRule(styles,
                ".ui-system .customer-detail--view .environment-detail .detail-section");
        String collapsible = cssRule(styles,
                ".ui-system .customer-detail--view .environment-detail > details.detail-section");
        String item = cssRule(styles,
                ".ui-system .customer-detail--view .detail-item");
        String value = cssRule(styles,
                ".ui-system .customer-detail--view .detail-value");
        String note = cssRule(styles,
                ".customer-detail--view .note-content");

        assertTrue(section.contains("padding: var(--space-20) var(--space-24);"));
        assertTrue(collapsible.contains("padding: 0;"));
        assertTrue(item.contains("border-bottom: 0;"));
        assertTrue(value.contains("color: var(--color-text-strong);"));
        assertTrue(note.contains("border: 0;"));
    }

    @Test
    void booleanInspectorFieldsUseNaturalLanguageStatusInsteadOfRawYn() throws Exception {
        Path webapp = Path.of("src/main/webapp");
        String fields = Files.readString(
                webapp.resolve("customers/_detail_sections.jspf"));
        String fieldTag = Files.readString(
                webapp.resolve("WEB-INF/tags/detailField.tag"));

        assertEquals(8, occurrences(fields, "booleanState=\"${true}\""));
        assertTrue(fieldTag.contains("name=\"booleanState\""));
        assertTrue(fieldTag.contains("detail-status--enabled"));
        assertTrue(fieldTag.contains("detail-status--disabled"));
        assertTrue(fieldTag.contains("aria-hidden=\"true\""));
        assertTrue(fieldTag.contains(">사용</span>"));
        assertTrue(fieldTag.contains(">미사용</span>"));
    }

    @Test
    void narrowInspectorKeepsShortFieldsComparableAndStacksLongValues() throws Exception {
        String styles = Files.readString(Path.of(
                "src/main/webapp/resources/css/pages/customer_detail.css"));
        int narrowBreakpoint = styles.indexOf("@media (max-width: 480px)");

        assertTrue(narrowBreakpoint >= 0);
        String narrowStyles = styles.substring(narrowBreakpoint);
        String item = cssRule(narrowStyles,
                ".ui-system .customer-detail--view .detail-item");
        String fullWidth = cssRule(narrowStyles,
                ".ui-system .customer-detail--view .detail-item.full-width");
        String label = cssRule(narrowStyles,
                ".ui-system .customer-detail--view .detail-label");

        assertTrue(item.contains(
                "grid-template-columns: minmax(100px, 112px) minmax(0, 1fr);"));
        assertTrue(fullWidth.contains("grid-template-columns: 1fr;"));
        assertTrue(label.contains("white-space: normal;"));
    }

    private static String cssRule(String css, String selector) {
        int selectorStart = css.indexOf(selector + " {");
        if (selectorStart < 0) {
            return "";
        }
        int blockStart = css.indexOf('{', selectorStart);
        int blockEnd = css.indexOf('}', blockStart);
        return css.substring(blockStart + 1, blockEnd);
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
