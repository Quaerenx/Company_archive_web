package com.company.customerhistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerHistoryConfigTest {
    @Test
    void developmentUsesExternalDefault() {
        assertEquals(
                Path.of(CustomerHistoryConfig.DEVELOPMENT_DEFAULT),
                CustomerHistoryConfig.resolveRoot("dev", null));
    }

    @Test
    void nonDevelopmentRequiresExplicitAbsoluteRoot() {
        assertThrows(
                IllegalStateException.class,
                () -> CustomerHistoryConfig.resolveRoot("prod", null));
        assertThrows(
                IllegalStateException.class,
                () -> CustomerHistoryConfig.resolveRoot("prod", "relative/history"));
    }

    @Test
    void configuredAbsoluteRootIsNormalized() {
        assertEquals(
                Path.of("/srv/archive/customer-history"),
                CustomerHistoryConfig.resolveRoot(
                        "prod", "/srv/archive/data/../customer-history"));
    }
}
