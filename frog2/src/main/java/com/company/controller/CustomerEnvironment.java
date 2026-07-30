package com.company.controller;

enum CustomerEnvironment {
    PROD("prod"),
    STAGING("stg"),
    DEVELOPMENT("dev");

    private final String externalValue;

    CustomerEnvironment(String externalValue) {
        this.externalValue = externalValue;
    }

    static CustomerEnvironment fromExternalValue(String value) {
        if (value == null || value.isBlank()) {
            return PROD;
        }

        String normalized = value.trim();
        for (CustomerEnvironment environment : values()) {
            if (environment.externalValue.equalsIgnoreCase(normalized)) {
                return environment;
            }
        }
        throw new IllegalArgumentException("Unsupported customer environment");
    }

    String externalValue() {
        return externalValue;
    }
}
