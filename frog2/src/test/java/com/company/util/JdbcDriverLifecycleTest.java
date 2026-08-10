package com.company.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class JdbcDriverLifecycleTest {
    @Test
    void deregistersOnlyDriversOwnedByTheApplicationClassLoader()
            throws Exception {
        Driver driver = new TestDriver();
        DriverManager.registerDriver(driver);
        try {
            int deregistered = JdbcDriverLifecycle.deregisterDrivers(
                    TestDriver.class.getClassLoader());

            assertTrue(deregistered >= 1);
            assertFalse(DriverManager.drivers().anyMatch(
                    registered -> registered == driver));
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    private static final class TestDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) {
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return false;
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(
                String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger()
                throws java.sql.SQLFeatureNotSupportedException {
            throw new java.sql.SQLFeatureNotSupportedException();
        }
    }
}
