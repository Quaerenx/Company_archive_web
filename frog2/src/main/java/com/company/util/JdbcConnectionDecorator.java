package com.company.util;

import java.sql.Connection;
import java.sql.SQLException;

final class JdbcConnectionDecorator {
    private JdbcConnectionDecorator() {
    }

    static Connection decorate(
            Connection rawConnection,
            int queryTimeoutSeconds,
            boolean readOnly) throws SQLException {
        try {
            Connection decorated = JdbcTiming.wrap(
                    rawConnection, queryTimeoutSeconds);
            return readOnly
                    ? ReadOnlyJdbcGuard.wrap(decorated)
                    : decorated;
        } catch (SQLException | RuntimeException | Error exception) {
            closeAfterDecorationFailure(rawConnection, exception);
            throw exception;
        }
    }

    private static void closeAfterDecorationFailure(
            Connection rawConnection, Throwable primaryFailure) {
        if (rawConnection == null) {
            return;
        }
        try {
            rawConnection.close();
        } catch (SQLException closeFailure) {
            primaryFailure.addSuppressed(closeFailure);
        }
    }
}
