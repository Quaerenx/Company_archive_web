package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FormLabelAccessibilityContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern EDIT_FIELD_TAG = Pattern.compile(
            "<t:customerDetailEditField\\b(.*?)/>",
            Pattern.DOTALL);
    private static final Pattern NAME_ATTRIBUTE = Pattern.compile(
            "\\bname=\"([^\"]+)\"");

    @Test
    void customerDetailEditLabelsEveryVisibleNamedControl() throws Exception {
        String fields = readCustomerDetailEditFields();
        String tag = read("WEB-INF/tags/customerDetailEditField.tag");
        Set<String> fieldNames = new HashSet<>();
        Matcher fieldTags = EDIT_FIELD_TAG.matcher(fields);
        int fieldCount = 0;

        while (fieldTags.find()) {
            String attributes = fieldTags.group(1);
            Matcher nameAttribute = NAME_ATTRIBUTE.matcher(attributes);
            assertTrue(nameAttribute.find(), "Missing name on edit field tag");
            assertTrue(attributes.contains("label=\""),
                    "Missing label on edit field: " + nameAttribute.group(1));
            assertTrue(fieldNames.add(nameAttribute.group(1)),
                    "Duplicate edit field: " + nameAttribute.group(1));
            fieldCount++;
        }

        assertEquals(47, fieldCount);
        assertEquals(47, fieldNames.size());
        assertTrue(tag.contains("value=\"${idPrefix}-${name}\""));
        assertTrue(tag.contains(
                "<label class=\"detail-label\" for=\"<c:out value='${fieldId}' />\""));
        assertEquals(3, occurrences(
                tag, "id=\"<c:out value='${fieldId}' />\""));
        assertEquals(3, occurrences(
                tag, "name=\"<c:out value='${name}' />\""));
    }

    @Test
    void readonlyMaintenanceAndTroubleshootingFieldsHaveLabels() throws Exception {
        String maintenance = read("maintenance/maintenance_edit.jsp");
        assertTrue(maintenance.contains(
                "<label for=\"maintenanceCreatedAt\">등록일시</label>"));
        assertTrue(maintenance.contains(
                "<input type=\"text\" id=\"maintenanceCreatedAt\""));
        assertTrue(maintenance.contains(
                "<label for=\"maintenanceUpdatedAt\">수정일시</label>"));
        assertTrue(maintenance.contains(
                "<input type=\"text\" id=\"maintenanceUpdatedAt\""));

        String troubleshooting = read(
                "WEB-INF/includes/_troubleshooting_form_fields.jspf");
        assertTrue(troubleshooting.contains(
                "<label for=\"troubleshooting_creator_display\">작성자</label>"));
        assertTrue(troubleshooting.contains(
                "<input type=\"text\" id=\"troubleshooting_creator_display\""));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static String readCustomerDetailEditFields() throws Exception {
        StringBuilder source = new StringBuilder();
        for (String section : new String[] {
                "summary", "meta", "vertica", "environment", "solutions", "other"}) {
            source.append(read(
                    "customers/_detail_edit_" + section + ".jspf"));
        }
        return source.toString();
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
