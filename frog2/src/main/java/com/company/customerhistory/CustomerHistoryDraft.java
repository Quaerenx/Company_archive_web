package com.company.customerhistory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record CustomerHistoryDraft(
        String customerName,
        LocalDate workDate,
        CustomerHistoryCategory category,
        String title,
        String actionSummary,
        CustomerHistoryStatus status) {

    public static final int MAX_CUSTOMER_NAME_LENGTH = 200;
    public static final int MAX_TITLE_LENGTH = 300;
    public static final int MAX_ACTION_LENGTH = 4_000;

    public CustomerHistoryDraft {
        customerName = required(
                customerName, "고객사를 선택해 주세요.", MAX_CUSTOMER_NAME_LENGTH);
        if (workDate == null) {
            throw new IllegalArgumentException("작업일을 입력해 주세요.");
        }
        if (category == null) {
            throw new IllegalArgumentException("작업 유형을 선택해 주세요.");
        }
        title = required(title, "이력을 입력해 주세요.", MAX_TITLE_LENGTH);
        actionSummary = required(
                actionSummary, "조치사항을 입력해 주세요.", MAX_ACTION_LENGTH);
        if (status == null) {
            throw new IllegalArgumentException("작업 상태를 선택해 주세요.");
        }
    }

    public static CustomerHistoryDraft from(
            String customerName,
            String workDate,
            String category,
            String title,
            String actionSummary,
            String status) {
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(required(
                    workDate, "작업일을 입력해 주세요.", 10));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("작업일 형식이 올바르지 않습니다.");
        }
        return new CustomerHistoryDraft(
                customerName,
                parsedDate,
                CustomerHistoryCategory.fromCode(category),
                title,
                actionSummary,
                CustomerHistoryStatus.fromCode(status));
    }

    private static String required(String value, String message, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    "입력값은 " + maximumLength + "자를 넘을 수 없습니다.");
        }
        return normalized;
    }
}
