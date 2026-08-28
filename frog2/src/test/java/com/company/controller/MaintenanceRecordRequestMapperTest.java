package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDTO;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceRecordRequestMapperTest {
    private final MaintenanceRecordRequestMapper mapper =
            new MaintenanceRecordRequestMapper();

    @Test
    void formOptionsExposeJavaBeanPropertiesForJspEl() throws Exception {
        PropertyDescriptor[] properties = Introspector
                .getBeanInfo(MaintenanceFormOptions.class)
                .getPropertyDescriptors();

        assertTrue(hasReadableProperty(properties, "customers"));
        assertTrue(hasReadableProperty(properties, "inspectors"));
    }

    @Test
    void canonicalizesLicenseValuesAndCalculatesPercentageOnTheServer() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_size_gb", "25 TB");
        parameters.put("license_usage_size", "13.9TB");
        parameters.put("license_usage_pct", "1%");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, " owner-1 ", options("25TB"));

        assertTrue(submission.valid());
        assertEquals("owner-1", submission.record().getCreatorUserId());
        assertEquals("Acme", submission.record().getCustomerName());
        assertEquals("Alice", submission.record().getInspectorName());
        assertEquals(Date.valueOf("2026-08-12"),
                submission.record().getInspectionDate());
        assertEquals("23.4.0-13", submission.record().getVerticaVersion());
        assertEquals("25", submission.record().getLicenseSizeGb());
        assertEquals("13.9", submission.record().getLicenseUsageSize());
        assertEquals("55.6", submission.record().getLicenseUsagePct());
    }

    @Test
    void preservesSubTerabyteChangesInTheStoredPercentage() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_usage_size", "52.464");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("80TB"));

        assertTrue(submission.valid());
        assertEquals("52.464", submission.record().getLicenseUsageSize());
        assertEquals("65.58", submission.record().getLicenseUsagePct());
    }

    @Test
    void acceptsLegacyGbAndPercentageSuffixesWithoutChangingTheContract() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_size_gb", "1024GB");
        parameters.put("license_usage_size", "512 GB");
        parameters.put("license_usage_pct", "50%");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("1024GB"));

        assertTrue(submission.valid());
        assertEquals("1", submission.record().getLicenseSizeGb());
        assertEquals("0.5", submission.record().getLicenseUsageSize());
        assertEquals("50.0", submission.record().getLicenseUsagePct());
    }

    @Test
    void rejectsUnknownReferencesAndInvalidDates() {
        Map<String, String> parameters = validParameters();
        parameters.put("customer_name", "Unknown");
        parameters.put("inspector_name", "Unknown User");
        parameters.put("inspection_date", "2026-02-30");
        parameters.put("license_size_gb", "5");
        parameters.put("license_usage_size", "8");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("5TB"));

        assertFalse(submission.valid());
        assertTrue(submission.fieldErrors().containsKey("customer_name"));
        assertTrue(submission.fieldErrors().containsKey("inspector_name"));
        assertTrue(submission.fieldErrors().containsKey("inspection_date"));
        assertNull(submission.record().getInspectionDate());
    }

    @Test
    void acceptsUsageAboveCapacityAndCalculatesOverOneHundredPercent() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_size_gb", "999");
        parameters.put("license_usage_size", "8");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("5TB"));

        assertTrue(submission.valid());
        assertEquals("5", submission.record().getLicenseSizeGb());
        assertEquals("8", submission.record().getLicenseUsageSize());
        assertEquals("160.0", submission.record().getLicenseUsagePct());
        assertFalse(submission.fieldErrors().containsKey(
                "license_usage_size"));
    }

    @Test
    void acceptsExplicitOverOneHundredPercentWhenNoCapacityIsAvailable() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_usage_pct", "105.1%");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options(null));

        assertTrue(submission.valid());
        assertEquals("105.1", submission.record().getLicenseUsagePct());
    }

    @Test
    void allowsNonCapacityLicenseModelsWithoutInventingTbUsage() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_usage_size", "999");
        parameters.put("license_usage_pct", "99");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("54nodes"));

        assertTrue(submission.valid());
        assertEquals("54nodes", submission.record().getLicenseSizeGb());
        assertNull(submission.record().getLicenseUsageSize());
        assertNull(submission.record().getLicenseUsagePct());
        assertFalse(submission.fieldErrors().containsKey(
                "license_size_gb"));
    }

    @Test
    void retainsZeroCapacityAndExtremePercentageSafetyLimits() {
        Map<String, String> zeroCapacity = validParameters();
        zeroCapacity.put("license_usage_size", "1");

        MaintenanceFormSubmission zeroCapacitySubmission = mapper.map(
                zeroCapacity::get, "owner-1", options("0"));

        assertFalse(zeroCapacitySubmission.valid());
        assertTrue(zeroCapacitySubmission.fieldErrors().containsKey(
                "license_usage_size"));

        Map<String, String> extremePercentage = validParameters();
        extremePercentage.put("license_usage_pct", "1000000.1");

        MaintenanceFormSubmission extremePercentageSubmission = mapper.map(
                extremePercentage::get, "owner-1", options(null));

        assertFalse(extremePercentageSubmission.valid());
        assertTrue(extremePercentageSubmission.fieldErrors().containsKey(
                "license_usage_pct"));

        Map<String, String> calculatedAboveLimit = validParameters();
        calculatedAboveLimit.put("license_usage_size", "10000.000004");

        MaintenanceFormSubmission calculatedAboveLimitSubmission =
                mapper.map(
                        calculatedAboveLimit::get,
                        "owner-1",
                        options("0.001TB"));

        assertFalse(calculatedAboveLimitSubmission.valid());
        assertTrue(calculatedAboveLimitSubmission.fieldErrors().containsKey(
                "license_usage_size"));
    }

    @Test
    void ignoresSubmittedVersionAndUsesTheCustomerMasterValue() {
        Map<String, String> parameters = validParameters();
        parameters.put("vertica_version", "v".repeat(51));
        parameters.put("note", "점검 메모");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("25TB"));

        assertTrue(submission.valid());
        assertEquals("점검 메모", submission.record().getNote());
        assertEquals(
                "23.4.0-13", submission.record().getVerticaVersion());
        assertFalse(submission.fieldErrors().containsKey("vertica_version"));
    }

    private static Map<String, String> validParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("customer_name", " Acme ");
        parameters.put("inspector_name", " Alice ");
        parameters.put("inspection_date", "2026-08-12");
        parameters.put("vertica_version", " 23.4.0-13 ");
        parameters.put("note", " 점검 완료 ");
        return parameters;
    }

    @Test
    void ignoresSubmittedCapacityAndUsesTheCustomerMasterValue() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_size_gb", "999");
        parameters.put("license_usage_size", "13.9");

        MaintenanceFormSubmission submission = mapper.map(
                parameters::get, "owner-1", options("25TB"));

        assertTrue(submission.valid());
        assertEquals("25", submission.record().getLicenseSizeGb());
        assertEquals("55.6", submission.record().getLicenseUsagePct());
    }

    @Test
    void updateKeepsTheCapacityStoredWithTheHistoricalRecord() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_size_gb", "999");
        parameters.put("license_usage_size", "5");

        MaintenanceFormSubmission submission = mapper.mapForUpdate(
                parameters::get,
                "owner-1",
                options("30TB"),
                17L,
                "20TB",
                "12.0.2-1",
                "5",
                "25.0");

        assertTrue(submission.valid());
        assertEquals("20", submission.record().getLicenseSizeGb());
        assertEquals("25.0", submission.record().getLicenseUsagePct());
        assertEquals(
                "12.0.2-1", submission.record().getVerticaVersion());
    }

    @Test
    void updatePreservesHistoricalValuesForNonCapacityLicenses() {
        Map<String, String> parameters = validParameters();
        parameters.put("license_usage_size", "999");
        parameters.put("license_usage_pct", "99");

        MaintenanceFormSubmission submission = mapper.mapForUpdate(
                parameters::get,
                "owner-1",
                options("54nodes"),
                18L,
                "54nodes",
                "12.0.2-1",
                "legacy-usage",
                "legacy-percentage");

        assertTrue(submission.valid());
        assertEquals("54nodes", submission.record().getLicenseSizeGb());
        assertEquals(
                "legacy-usage",
                submission.record().getLicenseUsageSize());
        assertEquals(
                "legacy-percentage",
                submission.record().getLicenseUsagePct());
    }

    private static MaintenanceFormOptions options(String licenseSize) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Acme");
        customer.setManagerName("Alice");
        customer.setSubManagerName("Bob");
        customer.setVerticaVersion("23.4.0-13");
        customer.setLicenseSize(licenseSize);
        return MaintenanceFormOptions.from(List.of(customer), null);
    }

    private static boolean hasReadableProperty(
            PropertyDescriptor[] properties, String name) {
        for (PropertyDescriptor property : properties) {
            if (name.equals(property.getName())
                    && property.getReadMethod() != null) {
                return true;
            }
        }
        return false;
    }
}
