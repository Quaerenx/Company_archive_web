package com.company.performance;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class JdbcConnectionAcquisition {
    private JdbcConnectionAcquisition() {
    }

    public static Connection acquire(ConnectionSupplier supplier)
            throws SQLException {
        return acquire(supplier, System::nanoTime);
    }

    static Connection acquire(
            ConnectionSupplier supplier,
            LongSupplier nanoTime) throws SQLException {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(nanoTime, "nanoTime");
        long started = nanoTime.getAsLong();
        try {
            return supplier.get();
        } finally {
            RequestPerformanceContext.recordDbAcquisition(
                    Math.max(0, nanoTime.getAsLong() - started));
        }
    }

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
