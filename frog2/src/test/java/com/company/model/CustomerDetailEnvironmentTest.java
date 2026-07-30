package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CustomerDetailEnvironmentTest {
    @Test
    void onlyKnownExternalValuesSelectEnvironment() {
        assertEquals(CustomerDetailEnvironment.PROD,
                CustomerDetailEnvironment.fromExternalValue(null));
        assertEquals(CustomerDetailEnvironment.PROD,
                CustomerDetailEnvironment.fromExternalValue("unknown_table"));
        assertEquals(CustomerDetailEnvironment.PROD,
                CustomerDetailEnvironment.fromExternalValue("prod"));
        assertEquals(CustomerDetailEnvironment.STAGING,
                CustomerDetailEnvironment.fromExternalValue("STG"));
        assertEquals(CustomerDetailEnvironment.DEVELOPMENT,
                CustomerDetailEnvironment.fromExternalValue(" dev "));
    }

    @Test
    void tableNamesAreFixedByTheEnum() {
        assertEquals("vertica_customer_detail",
                CustomerDetailEnvironment.PROD.tableName());
        assertEquals("vertica_customer_detail_stg",
                CustomerDetailEnvironment.STAGING.tableName());
        assertEquals("vertica_customer_detail_dev",
                CustomerDetailEnvironment.DEVELOPMENT.tableName());
    }
}
