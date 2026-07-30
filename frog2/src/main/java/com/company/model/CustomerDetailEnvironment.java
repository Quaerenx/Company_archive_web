package com.company.model;

enum CustomerDetailEnvironment {
    PROD("prod", "vertica_customer_detail"),
    STAGING("stg", "vertica_customer_detail_stg"),
    DEVELOPMENT("dev", "vertica_customer_detail_dev");

    private final String externalValue;
    private final String tableName;

    CustomerDetailEnvironment(String externalValue, String tableName) {
        this.externalValue = externalValue;
        this.tableName = tableName;
    }

    static CustomerDetailEnvironment fromExternalValue(String value) {
        if (value != null) {
            String normalized = value.trim();
            for (CustomerDetailEnvironment environment : values()) {
                if (environment.externalValue.equalsIgnoreCase(normalized)) {
                    return environment;
                }
            }
        }
        return PROD;
    }

    String externalValue() {
        return externalValue;
    }

    String tableName() {
        return tableName;
    }
}
