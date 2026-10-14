package com.company.model;

import com.company.util.LicenseRiskPolicy;
import java.time.LocalDate;

public record MaintenanceMonthState(
        String customerName,
        String inspectorName,
        LocalDate inspectionDate,
        LicenseRiskPolicy.Level licenseRiskLevel) {
}
