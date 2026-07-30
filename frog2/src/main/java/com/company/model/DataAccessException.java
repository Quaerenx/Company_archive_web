package com.company.model;

import java.sql.SQLException;
import java.util.Objects;

public final class DataAccessException extends RuntimeException {
    public enum Kind {
        GENERAL,
        READ_ONLY
    }

    private static final String READ_ONLY_SQL_STATE = "25006";
    private final Kind kind;

    private DataAccessException(Kind kind, String operation, SQLException cause) {
        super("Database operation failed: " + operation, cause);
        this.kind = kind;
    }

    public static DataAccessException from(SQLException cause) {
        return from("unspecified operation", cause);
    }

    public static DataAccessException from(String operation, SQLException cause) {
        Objects.requireNonNull(cause, "cause");
        Kind kind = hasSqlState(cause, READ_ONLY_SQL_STATE) ? Kind.READ_ONLY : Kind.GENERAL;
        String safeOperation = operation == null || operation.isBlank()
                ? "unspecified operation"
                : operation;
        return new DataAccessException(kind, safeOperation, cause);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isReadOnlyViolation() {
        return kind == Kind.READ_ONLY;
    }

    private static boolean hasSqlState(SQLException cause, String expectedState) {
        Throwable current = cause;
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current instanceof SQLException sqlException) {
                for (SQLException next = sqlException; next != null; next = next.getNextException()) {
                    if (expectedState.equals(next.getSQLState())) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
