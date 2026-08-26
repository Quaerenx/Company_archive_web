package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PhaseOneInteractionAccessibilityContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void fileUploadSurfaceOwnsTheFocusableFileInput() throws Exception {
        String page = read("WEB-INF/views/filerepo/upload.jsp");
        String css = read("resources/css/pages/upload.css");
        int labelStart = page.indexOf("<label class=\"upload-area\"");
        int input = page.indexOf("id=\"upload-files\"", labelStart);
        int labelEnd = page.indexOf("</label>", labelStart);

        assertTrue(labelStart >= 0);
        assertTrue(input > labelStart && input < labelEnd);
        assertTrue(css.contains(
                ".page-file-upload .upload-area:focus-within"));
        assertTrue(css.contains(
                "outline: 2px solid var(--color-focus);"));
    }

    @Test
    void maintenanceErrorsHaveStableDescriptionsWithoutReplacingHelp()
            throws Exception {
        String fields = read("WEB-INF/includes/maintenance_form_fields.jspf");
        String script = read("resources/js/pages/maintenance_form.js");

        assertDescription(fields, "maintenanceCustomerError");
        assertDescription(fields, "maintenanceVersionError");
        assertDescription(fields, "maintenanceInspectorError");
        assertDescription(fields, "maintenanceInspectionDateError");
        assertDescription(fields, "maintenanceCapacityError");
        assertDescription(fields, "maintenanceLicenseUsageError");
        assertDescription(fields, "maintenanceNoteError");

        assertTrue(fields.contains(
                "aria-describedby=\"maintenanceSelectedDateLabel${not empty fieldErrors.inspection_date ? ' maintenanceInspectionDateError' : ''}\""));
        assertTrue(fields.contains("id=\"maintenanceInlineCalendar\""));
        assertTrue(fields.contains(
                "aria-describedby=\"maintenanceVersionHelp${not empty fieldErrors.vertica_version ? ' maintenanceVersionError' : ''}\""));
        assertTrue(fields.contains(
                "aria-describedby=\"maintenanceCapacityHelp${not empty fieldErrors.license_size_gb ? ' maintenanceCapacityError' : ''}\""));
        assertTrue(script.contains(
                "describedBy.push('maintenanceUnsupportedCapacityHelp')"));
        assertTrue(script.contains(
                "describedBy.push('maintenanceLicenseUsageError')"));
    }

    @Test
    void calendarRerenderRestoresFocusOnlyWhenItStartedInTheGrid()
            throws Exception {
        String script = read("resources/js/pages/maintenance_calendar.js");
        int handler = script.indexOf("function handleDateChange()");
        int nextFunction = script.indexOf(
                "function changeMonth", handler);
        String handlerBody = script.substring(handler, nextFunction);

        assertTrue(handlerBody.contains(
                "grid.contains(calendarDocument.activeElement)"));
        assertTrue(handlerBody.indexOf("render(selectedValue)")
                < handlerBody.indexOf(
                        "focusButton(grid, selectedValue)"));
    }

    private static void assertDescription(String source, String id) {
        assertTrue(source.contains("id=\"" + id + "\""));
        assertTrue(source.contains(id));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
