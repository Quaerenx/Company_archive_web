package com.company.util;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

final class JdbcDriverLifecycle {
    private JdbcDriverLifecycle() {
    }

    static int deregisterDrivers(ClassLoader applicationClassLoader)
            throws SQLException {
        if (applicationClassLoader == null) {
            return 0;
        }

        List<Driver> ownedDrivers = new ArrayList<>();
        Enumeration<Driver> registered = DriverManager.getDrivers();
        while (registered.hasMoreElements()) {
            Driver driver = registered.nextElement();
            if (driver.getClass().getClassLoader() == applicationClassLoader) {
                ownedDrivers.add(driver);
            }
        }

        int deregistered = 0;
        SQLException failure = null;
        for (Driver driver : ownedDrivers) {
            try {
                DriverManager.deregisterDriver(driver);
                deregistered++;
            } catch (SQLException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return deregistered;
    }
}
