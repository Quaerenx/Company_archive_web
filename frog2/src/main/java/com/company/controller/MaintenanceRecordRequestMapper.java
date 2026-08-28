package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceRecordDTO;
import com.company.util.StrictDateParser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MaintenanceRecordRequestMapper {
    private static final int VERSION_MAX_LENGTH = 50;
    private static final int NOTE_MAX_LENGTH = 65_000;
    private static final BigDecimal MAX_TERABYTES =
            new BigDecimal("1000000");
    private static final BigDecimal MAX_PERCENTAGE =
            new BigDecimal("1000000.0");
    private static final Pattern TERABYTE_VALUE = Pattern.compile(
            "^([+-]?\\d+(?:\\.\\d{1,6})?)\\s*(TB|GB)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENTAGE_VALUE = Pattern.compile(
            "^([+-]?\\d+(?:\\.\\d{1,6})?)\\s*%?$");

    MaintenanceFormSubmission map(
            Function<String, String> parameter,
            String creatorUserId,
            MaintenanceFormOptions options) {
        return mapWithFixedSnapshot(
                parameter,
                creatorUserId,
                options,
                null,
                null,
                null,
                null,
                false);
    }

    private MaintenanceFormSubmission mapWithFixedSnapshot(
            Function<String, String> parameter,
            String creatorUserId,
            MaintenanceFormOptions options,
            String fixedLicenseSize,
            String fixedVersion,
            String fixedLicenseUsage,
            String fixedLicensePercentage,
            boolean useHistoricalSnapshot) {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(options, "options");
        Map<String, String> errors = new LinkedHashMap<>();
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCreatorUserId(trimToNull(creatorUserId));

        String customerName = trimToNull(parameter.apply("customer_name"));
        record.setCustomerName(customerName);
        CustomerDTO customer = options.customer(customerName);
        if (customerName == null) {
            errors.put("customer_name", "고객사를 선택해 주세요.");
        } else if (customer == null) {
            errors.put("customer_name", "등록 가능한 고객사가 아닙니다.");
        }

        String inspectorName = trimToNull(parameter.apply("inspector_name"));
        record.setInspectorName(inspectorName);
        if (inspectorName == null) {
            errors.put("inspector_name", "점검자를 선택해 주세요.");
        } else if (!options.hasInspector(inspectorName)) {
            errors.put("inspector_name", "등록 가능한 점검자가 아닙니다.");
        }

        String inspectionDate = trimToNull(parameter.apply("inspection_date"));
        record.setInspectionDate(
                StrictDateParser.parseSqlDateOrNull(inspectionDate));
        if (inspectionDate == null) {
            errors.put("inspection_date", "점검일을 입력해 주세요.");
        } else if (record.getInspectionDate() == null) {
            errors.put("inspection_date", "올바른 점검일을 입력해 주세요.");
        }

        String version = trimToNull(useHistoricalSnapshot
                ? fixedVersion
                : customer == null ? null : customer.getVerticaVersion());
        record.setVerticaVersion(version);
        if (length(version) > VERSION_MAX_LENGTH) {
            errors.put(
                    "vertica_version",
                    "Vertica 버전은 50자 이하로 입력해 주세요.");
        }

        String note = trimToNull(parameter.apply("note"));
        record.setNote(note);
        if (length(note) > NOTE_MAX_LENGTH) {
            errors.put(
                    "note", "점검 메모는 65,000자 이하로 입력해 주세요.");
        }

        String rawCapacity = trimToNull(useHistoricalSnapshot
                ? fixedLicenseSize
                : customer == null ? null : customer.getLicenseSize());
        Map<String, String> capacityErrors = new LinkedHashMap<>();
        BigDecimal capacity = parseTerabytes(
                rawCapacity,
                "license_size_gb",
                "전체 용량",
                capacityErrors);
        boolean capacityUsesSupportedUnit = rawCapacity == null
                || capacityErrors.isEmpty();
        if (capacityUsesSupportedUnit) {
            errors.putAll(capacityErrors);
        }
        String rawUsage = capacityUsesSupportedUnit
                ? trimToNull(parameter.apply("license_usage_size"))
                : useHistoricalSnapshot ? trimToNull(fixedLicenseUsage) : null;
        String rawPercentage = capacityUsesSupportedUnit
                ? trimToNull(parameter.apply("license_usage_pct"))
                : useHistoricalSnapshot
                        ? trimToNull(fixedLicensePercentage)
                        : null;
        BigDecimal usage = capacityUsesSupportedUnit
                ? parseTerabytes(
                        rawUsage, "license_usage_size", "사용량", errors)
                : null;

        record.setLicenseSizeGb(
                capacity == null ? rawCapacity : decimal(capacity));
        record.setLicenseUsageSize(
                usage == null ? rawUsage : decimal(usage));

        String percentage = null;
        if (capacity != null && usage != null) {
            if (capacity.signum() == 0) {
                if (usage.signum() > 0) {
                    errors.put(
                            "license_usage_size",
                            "전체 용량이 0일 때 사용량을 입력할 수 없습니다.");
                } else {
                    percentage = "0.0";
                }
            } else {
                BigDecimal percentageNumerator = usage.multiply(
                        BigDecimal.valueOf(100));
                if (percentageNumerator.compareTo(
                        capacity.multiply(MAX_PERCENTAGE)) > 0) {
                    errors.put(
                            "license_usage_size",
                            "계산된 사용률의 입력 범위를 확인해 주세요.");
                } else {
                    percentage = percentageNumerator
                        .divide(capacity, 2, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();
                    percentage = percentageDecimal(
                            new BigDecimal(percentage));
                }
            }
        } else if (capacityUsesSupportedUnit && rawPercentage != null) {
            BigDecimal parsedPercentage = parsePercentage(
                    rawPercentage, errors);
            if (parsedPercentage != null) {
                percentage = percentageDecimal(parsedPercentage
                        .setScale(2, RoundingMode.HALF_UP));
            }
        }
        record.setLicenseUsagePct(
                percentage == null ? rawPercentage : percentage);

        return new MaintenanceFormSubmission(record, errors);
    }

    MaintenanceFormSubmission mapForUpdate(
            Function<String, String> parameter,
            String creatorUserId,
            MaintenanceFormOptions options,
            Long maintenanceId,
            String fixedLicenseSize,
            String fixedVersion,
            String fixedLicenseUsage,
            String fixedLicensePercentage) {
        MaintenanceFormSubmission submission = mapWithFixedSnapshot(
                parameter,
                creatorUserId,
                options,
                fixedLicenseSize,
                fixedVersion,
                fixedLicenseUsage,
                fixedLicensePercentage,
                true);
        submission.record().setMaintenanceId(maintenanceId);
        return submission;
    }

    static String normalizeTerabytesForInput(String value) {
        Map<String, String> ignored = new LinkedHashMap<>();
        BigDecimal parsed = parseTerabytes(
                trimToNull(value), "value", "값", ignored);
        return parsed == null ? trimToNull(value) : decimal(parsed);
    }

    static String normalizePercentageForInput(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = PERCENTAGE_VALUE.matcher(normalized);
        if (!matcher.matches()) {
            return normalized;
        }
        try {
            BigDecimal percentage = new BigDecimal(matcher.group(1));
            return percentage.setScale(1, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (NumberFormatException exception) {
            return normalized;
        }
    }

    private static BigDecimal parseTerabytes(
            String value,
            String field,
            String label,
            Map<String, String> errors) {
        if (value == null) {
            return null;
        }
        Matcher matcher = TERABYTE_VALUE.matcher(value);
        if (!matcher.matches()) {
            errors.put(field, label + "은 숫자로 입력해 주세요.");
            return null;
        }
        try {
            BigDecimal result = new BigDecimal(matcher.group(1));
            if (result.signum() < 0 || result.compareTo(MAX_TERABYTES) > 0) {
                errors.put(field, label + "의 입력 범위를 확인해 주세요.");
                return null;
            }
            if ("GB".equalsIgnoreCase(matcher.group(2))) {
                result = result.divide(
                        BigDecimal.valueOf(1024), 6, RoundingMode.HALF_UP);
            }
            return result.stripTrailingZeros();
        } catch (NumberFormatException exception) {
            errors.put(field, label + "은 숫자로 입력해 주세요.");
            return null;
        }
    }

    private static BigDecimal parsePercentage(
            String value, Map<String, String> errors) {
        Matcher matcher = PERCENTAGE_VALUE.matcher(value);
        if (!matcher.matches()) {
            errors.put(
                    "license_usage_pct", "사용률은 숫자로 입력해 주세요.");
            return null;
        }
        try {
            BigDecimal percentage = new BigDecimal(matcher.group(1));
            if (percentage.signum() < 0
                    || percentage.compareTo(MAX_PERCENTAGE) > 0) {
                errors.put(
                        "license_usage_pct",
                        "사용률의 입력 범위를 확인해 주세요.");
                return null;
            }
            return percentage;
        } catch (NumberFormatException exception) {
            errors.put(
                    "license_usage_pct", "사용률은 숫자로 입력해 주세요.");
            return null;
        }
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String percentageDecimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return (normalized.scale() < 1 ? normalized.setScale(1) : normalized)
                .toPlainString();
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
