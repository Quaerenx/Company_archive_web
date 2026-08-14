package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceFormHistoryContext;
import com.company.model.MaintenanceRecordDTO;
import java.sql.Date;
import org.junit.jupiter.api.Test;

class MaintenanceFormContextJsonTest {
    @Test
    void encodesDefaultsAndHistoryWithoutLeakingInvalidJson() {
        CustomerDTO customer = new CustomerDTO();
        customer.setManagerName("  Alice  ");
        customer.setSubManagerName("Bob");
        customer.setVerticaVersion("23.4\"stable");
        customer.setLicenseSize("25TB");

        MaintenanceRecordDTO previous = new MaintenanceRecordDTO();
        previous.setMaintenanceId(7L);
        previous.setInspectionDate(Date.valueOf("2026-07-20"));
        previous.setInspectorName("Alice\nQA");
        previous.setVerticaVersion("23.4");
        previous.setLicenseSizeGb("25TB");
        previous.setLicenseUsageSize("13.9TB");
        previous.setLicenseUsagePct("55.6%");

        String json = MaintenanceFormContextJson.encode(
                customer,
                new MaintenanceFormHistoryContext(previous, null));

        assertTrue(json.contains("\"defaultInspector\":\"Alice\""));
        assertTrue(json.contains(
                "\"defaultVersion\":\"23.4\\\"stable\""));
        assertTrue(json.contains("\"defaultLicenseSize\":\"25\""));
        assertTrue(json.contains("\"inspectionDate\":\"2026-07-20\""));
        assertTrue(json.contains("\"inspector\":\"Alice\\nQA\""));
        assertTrue(json.contains("\"licenseUsage\":\"13.9\""));
        assertTrue(json.contains("\"licensePercentage\":\"55.6\""));
        assertTrue(json.endsWith("\"duplicate\":null}"));
    }

    @Test
    void usesSubManagerAndNullsWhenPrimaryDefaultsAreMissing() {
        CustomerDTO customer = new CustomerDTO();
        customer.setManagerName(" ");
        customer.setSubManagerName(" Bob ");

        String json = MaintenanceFormContextJson.encode(
                customer, MaintenanceFormHistoryContext.empty());

        assertEquals(
                "{\"defaultInspector\":\"Bob\","
                        + "\"defaultVersion\":null,"
                        + "\"defaultLicenseSize\":null,"
                        + "\"previous\":null,\"duplicate\":null}",
                json);
    }
}
