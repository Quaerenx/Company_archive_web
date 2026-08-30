package com.company.mypage;

import java.util.Objects;

public final class WorkInboxItem {
    private final Severity severity;
    private final String customerName;
    private final String title;
    private final String detail;
    private final String path;

    public WorkInboxItem(
            Severity severity,
            String customerName,
            String title,
            String detail,
            String path) {
        this.severity = Objects.requireNonNull(severity, "severity");
        this.customerName = required(customerName, "customerName");
        this.title = required(title, "title");
        this.detail = required(detail, "detail");
        this.path = safePath(path);
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
}
