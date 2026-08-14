package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceFormHistoryContext;
import com.company.model.MaintenanceRecordDTO;
import com.company.util.StrictDateParser;
import com.company.web.JsonResponse;
import java.util.Objects;

final class MaintenanceFormContextJson {
    private MaintenanceFormContextJson() {
    }

    static String encode(
            CustomerDTO customer,
            MaintenanceFormHistoryContext context) {
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(context, "context");
        return new StringBuilder("{")
                .append("\"defaultInspector\":")
                .append(jsonString(firstNonBlank(
                        customer.getManagerName(),
                        customer.getSubManagerName())))
                .append(",\"defaultVersion\":")
                .append(jsonString(customer.getVerticaVersion()))
                .append(",\"defaultLicenseSize\":")
                .append(jsonString(
                        MaintenanceRecordRequestMapper
                                .normalizeTerabytesForInput(
                                        customer.getLicenseSize())))
                .append(",\"previous\":")
                .append(recordJson(context.previousRecord()))
                .append(",\"duplicate\":")
                .append(recordJson(context.duplicateRecord()))
                .append('}')
                .toString();
    }

    private static String recordJson(MaintenanceRecordDTO record) {
        if (record == null) {
            return "null";
        }
        return new StringBuilder("{")
                .append("\"id\":").append(record.getMaintenanceId())
                .append(",\"inspectionDate\":")
                .append(jsonString(StrictDateParser.formatDate(
                        record.getInspectionDate())))
                .append(",\"inspector\":")
                .append(jsonString(record.getInspectorName()))
                .append(",\"version\":")
                .append(jsonString(record.getVerticaVersion()))
                .append(",\"licenseSize\":")
                .append(jsonString(
                        MaintenanceRecordRequestMapper
                                .normalizeTerabytesForInput(
                                        record.getLicenseSizeGb())))
                .append(",\"licenseUsage\":")
                .append(jsonString(
                        MaintenanceRecordRequestMapper
                                .normalizeTerabytesForInput(
                                        record.getLicenseUsageSize())))
                .append(",\"licensePercentage\":")
                .append(jsonString(
                        MaintenanceRecordRequestMapper
                                .normalizePercentageForInput(
                                        record.getLicenseUsagePct())))
                .append('}')
                .toString();
    }

    private static String jsonString(String value) {
        return value == null
                ? "null"
                : "\"" + JsonResponse.escape(value) + "\"";
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }
}
