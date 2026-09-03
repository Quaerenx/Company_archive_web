package com.company.mypage;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class WorkInboxItem {
    private final Severity severity;
    private final Type type;
    private final String customerName;
    private final String title;
    private final String detail;
    private final String path;
    private final String actionLabel;
    private final LocalDate referenceDate;
    private final LocalDate dueDate;
    private final String timelineLabel;

    public WorkInboxItem(
            Severity severity,
            String customerName,
            String title,
            String detail,
            String path) {
        this(
                severity,
                Type.MISSING_INFORMATION,
                customerName,
                title,
                detail,
                path,
                "확인하기",
                null,
                null,
                LocalDate.now());
    }

    public WorkInboxItem(
            Severity severity,
            Type type,
            String customerName,
            String title,
            String detail,
            String path,
            String actionLabel,
            LocalDate referenceDate,
            LocalDate dueDate,
            LocalDate today) {
        this.severity = Objects.requireNonNull(severity, "severity");
        this.type = Objects.requireNonNull(type, "type");
        this.customerName = required(customerName, "customerName");
        this.title = required(title, "title");
        this.detail = required(detail, "detail");
        this.path = safePath(path);
        this.actionLabel = required(actionLabel, "actionLabel");
        this.referenceDate = referenceDate;
        this.dueDate = dueDate;
        this.timelineLabel = timelineLabel(
                referenceDate,
                dueDate,
                Objects.requireNonNull(today, "today"));
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSeverityLabel() {
        return severity.label();
    }

    public String getTone() {
        return severity.tone();
    }

    public Type getType() {
        return type;
    }

    public String getTypeCode() {
        return type.code();
    }

    public String getTypeLabel() {
        return type.label();
    }

    public String getItemKey() {
        return type.code() + ":" + customerName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getPath() {
        return path;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getTimelineLabel() {
        return timelineLabel;
    }

    private static String timelineLabel(
            LocalDate referenceDate,
            LocalDate dueDate,
            LocalDate today) {
        if (dueDate != null) {
            long days = ChronoUnit.DAYS.between(today, dueDate);
            if (days > 0) {
                return "D-" + days;
            }
            if (days == 0) {
                return "오늘 마감";
            }
            return Math.abs(days) + "일 경과";
        }
        if (referenceDate != null) {
            long days = Math.max(0, ChronoUnit.DAYS.between(referenceDate, today));
            return days == 0 ? "오늘 확인" : days + "일 전 기준";
        }
        return "상시 확인";
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static String safePath(String value) {
        String path = required(value, "path");
        if (!path.startsWith("/") || path.startsWith("//")) {
            throw new IllegalArgumentException(
                    "path must be context-relative");
        }
        return path;
    }

    public enum Severity {
        DANGER("위험", "danger", 0),
        WARNING("확인 필요", "warning", 1),
        INFO("정보 누락", "neutral", 2);

        private final String label;
        private final String tone;
        private final int order;

        Severity(String label, String tone, int order) {
            this.label = label;
            this.tone = tone;
            this.order = order;
        }

        public String label() {
            return label;
        }

        public String tone() {
            return tone;
        }

        public int order() {
            return order;
        }
    }

    public enum Type {
        MAINTENANCE_MISSING("maintenance", "정기점검 미진행"),
        LICENSE_RISK("license", "라이선스 위험"),
        MISSING_INFORMATION("missing-info", "정보 누락");

        private final String code;
        private final String label;

        Type(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String code() {
            return code;
        }

        public String label() {
            return label;
        }
    }
}
