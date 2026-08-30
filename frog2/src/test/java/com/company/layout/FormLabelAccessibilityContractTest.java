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
    private static final Pattern NAMED_CONTROL = Pattern.compile(
            "<(input|select|textarea)\\b([^>]*\\bname=\"([^\"]+)\"[^>]*)>",
            Pattern.DOTALL);
    private static final Pattern ID_ATTRIBUTE = Pattern.compile(
            "\\bid=\"([^\"]+)\"");
    private static final Pattern LABEL_TARGET = Pattern.compile(
            "<label\\b[^>]*\\bfor=\"([^\"]+)\"",
            Pattern.DOTALL);

    @Test
    void customerDetailEditLabelsEveryVisibleNamedControl() throws Exception {
        String page = readCustomerDetailEditForm();
        Set<String> labelTargets = labelTargets(page);
        Matcher controls = NAMED_CONTROL.matcher(page);
        int visibleControlCount = 0;

        while (controls.find()) {
            String attributes = controls.group(2);
            if (attributes.contains("type=\"hidden\"")) {
                continue;
            }

            String name = controls.group(3);
            Matcher idAttribute = ID_ATTRIBUTE.matcher(attributes);
            assertTrue(idAttribute.find(), "Missing id for control: " + name);
            String id = idAttribute.group(1);
            assertEquals(name, id, "Control id should remain stable with its name: " + name);
            assertTrue(labelTargets.contains(id), "Missing label for control: " + name);
            visibleControlCount++;
        }

        assertEquals(49, visibleControlCount);
        assertEquals(49, labelTargets.size());
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

    private static Set<String> labelTargets(String source) {
        Set<String> targets = new HashSet<>();
        Matcher labels = LABEL_TARGET.matcher(source);
        while (labels.find()) {
            targets.add(labels.group(1));
        }
        return targets;
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static String readCustomerDetailEditForm() throws Exception {
        StringBuilder source = new StringBuilder(
                read("customers/customers_detail_edit.jsp"));
        for (String section : new String[] {
                "meta", "vertica", "environment", "solutions", "other"}) {
            source.append(read(
                    "customers/_detail_edit_" + section + ".jspf"));
        }
        return source.toString();
    }
}
