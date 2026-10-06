package com.company.model;

import java.sql.Connection;
import java.sql.SQLException;

final class JdbcTransaction {
    private JdbcTransaction() {
    }

    static boolean execute(Connection connection, Work work) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        boolean safeToRestore = true;
        Throwable failure = null;
        try {
            if (autoCommit) {
                connection.setAutoCommit(false);
            }
            boolean result = work.execute();
            if (result) connection.commit();
            else connection.rollback();
            return result;
        } catch (SQLException | RuntimeException | Error exception) {
            failure = exception;
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                safeToRestore = false;
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        } finally {
            if (autoCommit && safeToRestore) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException restoreFailure) {
                    if (failure == null) {
                        throw restoreFailure;
                    }
                    failure.addSuppressed(restoreFailure);
                }
            }
        }
    }

    @FunctionalInterface
    interface Work {
        boolean execute() throws SQLException;
    }
}
