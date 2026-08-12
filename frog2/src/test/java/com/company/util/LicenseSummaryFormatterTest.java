package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.model.MaintenanceRecordDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LicenseSummaryFormatterTest {
    @Test
    void preservesTerabyteSummaryText() {
        MaintenanceRecordDTO record = record("12TB", "3TB", "25%");

        assertEquals(
                "12.00TB 중 3.00TB 총 25.0% 사용 중",
                LicenseSummaryFormatter.format(record));
    }

    @Test
    void convertsGigabytesAndCalculatesMissingPercentage() {
        MaintenanceRecordDTO record = record("2048 GB", "512GB", null);

        assertEquals(
                "2.00TB 중 0.50TB 총 25.0% 사용 중",
                LicenseSummaryFormatter.format(record));
        assertEquals(
                "0.5",
                LicenseSummaryFormatter.formatUsageTerabytes(record));
        assertEquals(
                "2",
                LicenseSummaryFormatter.formatCapacityTerabytes(record));
        assertEquals(
                25,
                LicenseSummaryFormatter.resolveRoundedUsagePercentage(record));
        assertEquals(25.0, LicenseSummaryFormatter.resolveUsagePercentage(record));
    }

    @Test
    void keepsLegacyUnitlessValuesAsTerabytes() {
        MaintenanceRecordDTO record = record("2", "1", "50");

        assertEquals(
                "2.00TB 중 1.00TB 총 50.0% 사용 중",
                LicenseSummaryFormatter.format(record));
    }

    @Test
    void exposesAStableOneDecimalPercentageForHistoryViews() {
        MaintenanceRecordDTO record = record("25", "13.9", null);

        assertEquals(
                new BigDecimal("55.6"),
                LicenseSummaryFormatter
                        .resolveUsagePercentageOneDecimal(record));
        assertEquals(
                new BigDecimal("55.6"),
                LicenseSummaryFormatter
                        .resolveUsageProgressPercentageOneDecimal(record));
    }

    @Test
    void progressPercentageIsRoundedAndBoundedForVisualDisplay() {
        assertEquals(25, LicenseSummaryFormatter
                .resolveUsageProgressPercentage(
                        record("12", "3", "24.6")));
        assertEquals(0, LicenseSummaryFormatter
                .resolveUsageProgressPercentage(
                        record("12", "3", "-7")));
        assertEquals(100, LicenseSummaryFormatter
                .resolveUsageProgressPercentage(
                        record("12", "13", "108")));
        assertNull(LicenseSummaryFormatter
                .resolveUsageProgressPercentage(
                        new MaintenanceRecordDTO()));
    }

    @Test
    void returnsNullWhenNoLicenseValueExists() {
        assertNull(LicenseSummaryFormatter.format(new MaintenanceRecordDTO()));
        assertNull(LicenseSummaryFormatter.format(null));
        assertNull(LicenseSummaryFormatter.formatUsageTerabytes(null));
        assertNull(LicenseSummaryFormatter.formatCapacityTerabytes(null));
        assertNull(LicenseSummaryFormatter.resolveRoundedUsagePercentage(null));
        assertNull(LicenseSummaryFormatter.resolveUsagePercentage(new MaintenanceRecordDTO()));
        assertNull(LicenseSummaryFormatter.resolveUsagePercentage(null));
        assertNull(LicenseSummaryFormatter.parseNumber("not-a-number"));
    }

    private static MaintenanceRecordDTO record(
            String size, String usage, String percentage) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setLicenseSizeGb(size);
        record.setLicenseUsageSize(usage);
        record.setLicenseUsagePct(percentage);
        return record;
    }

}
