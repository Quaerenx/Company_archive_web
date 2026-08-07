package com.company.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceScheduleMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260804_07__create_customer_maintenance_schedule.sql");
    private static final Path KONKUK_OVERRIDE = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260804_08__set_konkuk_hospital_quarterly_schedule.sql");

    @Test
    void migrationKeepsAmbiguousCustomersMonthlyAndUsesHistoryForAnchors()
            throws Exception {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains(
                "CREATE TABLE IF NOT EXISTS customer_maintenance_schedule"));
        assertTrue(sql.contains("interval_months IN (1, 3)"));
        assertTrue(sql.contains("dominant.residue_month_count >= 3"));
        assertTrue(sql.contains(
                "DATEDIFF(month, history.first_month, history.last_month) >= 6"));
        assertTrue(sql.contains(
                ">= history.observed_month_count * 80"));
        assertTrue(sql.contains(
                "CASE WHEN quarterly.customer_name IS NULL THEN 1 ELSE 3 END"));
        assertTrue(sql.contains("dominant.first_residue_month AS anchor_month"));
        assertTrue(sql.contains("AND NOT EXISTS"));
    }

    @Test
    void konkukOverrideUsesMarchQuarterlyResidueAndOneExactCustomer()
            throws Exception {
        String sql = Files.readString(KONKUK_OVERRIDE);

        assertTrue(sql.contains("interval_months = 3"));
        assertTrue(sql.contains("anchor_month = DATE '2000-03-01'"));
        assertTrue(sql.contains("WHERE customer_name = '건국대병원'"));
    }
}
