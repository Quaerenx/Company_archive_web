package com.company.customerhistory;

import java.util.Locale;

public enum CustomerHistoryStatus {
    IN_PROGRESS("in_progress", "진행 중", "warning"),
    COMPLETED("completed", "완료", "success");

    private final String code;
    private final String label;
    private final String tone;

    CustomerHistoryStatus(String code, String label, String tone) {
        this.code = code;
        this.label = label;
        this.tone = tone;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }

    public static CustomerHistoryStatus fromCode(String value) {
        String normalized = value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
        for (CustomerHistoryStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("작업 상태가 올바르지 않습니다.");
    }
}
