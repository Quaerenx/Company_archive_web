package com.company.customerhistory;

import java.util.Locale;

public enum CustomerHistoryCategory {
    INCIDENT("incident", "장애"),
    UPGRADE("upgrade", "업그레이드"),
    EXPANSION("expansion", "증설"),
    OTHER("other", "기타 작업");

    private final String code;
    private final String label;

    CustomerHistoryCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static CustomerHistoryCategory fromCode(String value) {
        String normalized = value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
        for (CustomerHistoryCategory category : values()) {
            if (category.code.equals(normalized)) {
                return category;
            }
        }
        throw new IllegalArgumentException("작업 유형이 올바르지 않습니다.");
    }
}
