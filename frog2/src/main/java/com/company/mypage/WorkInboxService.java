package com.company.mypage;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.MaintenanceSchedule;
import com.company.mypage.WorkInboxItem.Severity;
import com.company.util.LicenseRiskPolicy;
import com.company.util.LicenseSummaryFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class WorkInboxService {
    private static final List<CustomerField> CRITICAL_FIELDS = List.of(
            new CustomerField("Vertica 버전", CustomerDTO::getVerticaVersion),
            new CustomerField("DB명", CustomerDTO::getDbName),
            new CustomerField("노드 수", CustomerDTO::getNodes),
            new CustomerField("전체 용량", CustomerDTO::getLicenseSize),
            new CustomerField("SAID", CustomerDTO::getSaid));

    public List<CustomerDTO> assignedCustomers(
            String assigneeName, List<CustomerDTO> maintenanceCustomers) {
        String normalizedAssignee = normalizeName(assigneeName);
        if (normalizedAssignee == null || maintenanceCustomers == null) {
            return List.of();
        }
        LinkedHashMap<String, CustomerDTO> assigned = new LinkedHashMap<>();
        for (CustomerDTO customer : maintenanceCustomers) {
            if (customer == null || isBlank(customer.getCustomerName())) {
                continue;
            }
            if (normalizedAssignee.equals(normalizeName(
                            customer.getManagerName()))
                    || normalizedAssignee.equals(normalizeName(
                            customer.getSubManagerName()))) {
                assigned.putIfAbsent(
                        customer.getCustomerName().strip(), customer);
            }
        }
        return List.copyOf(assigned.values());
    }

    public WorkInbox build(
            List<CustomerDTO> assignedCustomers,
            List<MaintenanceCustomerAssignment> assignments,
            List<MaintenanceRecordDTO> currentMonthRecords,
            List<MaintenanceRecordDTO> latestRecords,
            LocalDate today,
            int displayLimit) {
        Objects.requireNonNull(assignedCustomers, "assignedCustomers");
        Objects.requireNonNull(assignments, "assignments");
        Objects.requireNonNull(currentMonthRecords, "currentMonthRecords");
        Objects.requireNonNull(latestRecords, "latestRecords");
        Objects.requireNonNull(today, "today");

        YearMonth currentMonth = YearMonth.from(today);
        Map<String, MaintenanceSchedule> schedules = assignments.stream()
                .filter(Objects::nonNull)
                .filter(assignment -> !isBlank(assignment.customerName()))
                .collect(Collectors.toMap(
                        assignment -> assignment.customerName().strip(),
                        MaintenanceCustomerAssignment::schedule,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Set<String> completedCustomers = currentMonthRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> isCompletedThisMonth(
                        record.getInspectionDate(), currentMonth, today))
                .map(MaintenanceRecordDTO::getCustomerName)
                .filter(name -> !isBlank(name))
                .map(String::strip)
                .collect(Collectors.toSet());
        Map<String, MaintenanceRecordDTO> latestByCustomer = latestRecords.stream()
                .filter(Objects::nonNull)
                .filter(record -> !isBlank(record.getCustomerName()))
                .collect(Collectors.toMap(
                        record -> record.getCustomerName().strip(),
                        record -> record,
                        WorkInboxService::newerRecord,
                        LinkedHashMap::new));

        List<WorkInboxItem> items = new ArrayList<>();
        for (CustomerDTO customer : assignedCustomers) {
            if (customer == null || isBlank(customer.getCustomerName())) {
                continue;
            }
            String customerName = customer.getCustomerName().strip();
            MaintenanceSchedule schedule = schedules.getOrDefault(
                    customerName, MaintenanceSchedule.monthlyDefault());
            if (schedule.isDue(currentMonth)
                    && !completedCustomers.contains(customerName)) {
                items.add(new WorkInboxItem(
                        Severity.WARNING,
                        customerName,
                        "이번 달 정기점검 미진행",
                        currentMonth.getYear() + "년 "
                                + currentMonth.getMonthValue()
                                + "월 점검 이력이 없습니다.",
                        maintenancePath(customerName)));
            }

            MaintenanceRecordDTO latest = latestByCustomer.get(customerName);
            addLicenseAlert(items, customerName, latest);

            List<String> missingFields = CRITICAL_FIELDS.stream()
                    .filter(field -> isMissing(field.value().get(customer)))
                    .map(CustomerField::label)
                    .toList();
            if (!missingFields.isEmpty()) {
                items.add(new WorkInboxItem(
                        Severity.INFO,
                        customerName,
                        "고객사 핵심 정보 누락",
                        String.join(" · ", missingFields) + " 미기재",
                        customerPath(customerName)));
            }
        }

        items.sort(Comparator
                .comparingInt((WorkInboxItem item) ->
                        item.getSeverity().order())
                .thenComparing(WorkInboxItem::getCustomerName)
                .thenComparing(WorkInboxItem::getTitle));
        return WorkInbox.of(items, displayLimit);
    }

    private static void addLicenseAlert(
            List<WorkInboxItem> items,
            String customerName,
            MaintenanceRecordDTO latest) {
        LicenseRiskPolicy.Level risk =
                LicenseSummaryFormatter.resolveUsageRiskLevel(latest);
        if (risk != LicenseRiskPolicy.Level.WARNING
                && risk != LicenseRiskPolicy.Level.RISK) {
            return;
        }
        String percentage = LicenseSummaryFormatter
                .resolveUsagePercentageOneDecimal(latest)
                .toPlainString();
        Severity severity = risk == LicenseRiskPolicy.Level.RISK
                ? Severity.DANGER
                : Severity.WARNING;
        String title = risk == LicenseRiskPolicy.Level.RISK
                ? "라이선스 사용률 위험"
                : "라이선스 사용률 경고";
        String detail = "최근 점검 사용률 " + percentage + "%"
                + (risk == LicenseRiskPolicy.Level.RISK
                        ? " · 허용 용량 초과"
                        : "");
        items.add(new WorkInboxItem(
                severity,
                customerName,
                title,
                detail,
                maintenancePath(customerName)));
    }

    private static MaintenanceRecordDTO newerRecord(
            MaintenanceRecordDTO first,
            MaintenanceRecordDTO second) {
        Date firstDate = first.getInspectionDate();
        Date secondDate = second.getInspectionDate();
        if (firstDate == null) {
            return secondDate == null ? first : second;
        }
        return secondDate != null && secondDate.after(firstDate)
                ? second
                : first;
    }

    private static boolean isCompletedThisMonth(
            Date inspectionDate,
            YearMonth currentMonth,
            LocalDate today) {
        if (inspectionDate == null) {
            return false;
        }
        LocalDate date = inspectionDate.toLocalDate();
        return YearMonth.from(date).equals(currentMonth)
                && !date.isAfter(today);
    }

    private static boolean isMissing(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return "-".equals(normalized)
                || "미등록".equals(normalized)
                || "n/a".equals(normalized);
    }

    private static String normalizeName(String value) {
        return isBlank(value)
                ? null
                : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String maintenancePath(String customerName) {
        return "/maintenance?view=history&customerName="
                + encode(customerName);
    }

    private static String customerPath(String customerName) {
        return "/customers?view=detail&customerName="
                + encode(customerName);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface CustomerValue {
        String get(CustomerDTO customer);
    }

    private record CustomerField(String label, CustomerValue value) {
    }
}
