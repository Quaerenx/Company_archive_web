package com.company.controller;

import com.company.model.CustomerDetailDTO;
import java.util.Objects;

public final class CustomerDetailEditEnvironmentView {
    private final String value;
    private final String label;
    private final CustomerDetailDTO detail;

    public CustomerDetailEditEnvironmentView(
            String value, String label, CustomerDetailDTO detail) {
        this.value = Objects.requireNonNull(value, "value");
        this.label = Objects.requireNonNull(label, "label");
        this.detail = detail;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public CustomerDetailDTO getDetail() {
        return detail;
    }
}
