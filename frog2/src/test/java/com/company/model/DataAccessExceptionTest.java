package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import org.junit.jupiter.api.Test;

class DataAccessExceptionTest {
    @Test
    void classifiesOrdinarySqlFailureAsGeneral() {
        SQLException cause = new SQLException("connection failed", "08001");

        DataAccessException exception = DataAccessException.from("load user", cause);

        assertEquals(DataAccessException.Kind.GENERAL, exception.getKind());
        assertFalse(exception.isReadOnlyViolation());
        assertSame(cause, exception.getCause());
    }

    @Test
    void classifiesReadOnlySqlStateFromNextException() {
        SQLException cause = new SQLException("outer", "HY000");
        cause.setNextException(new SQLNonTransientException("blocked", "25006"));

        DataAccessException exception = DataAccessException.from("save record", cause);

        assertEquals(DataAccessException.Kind.READ_ONLY, exception.getKind());
        assertTrue(exception.isReadOnlyViolation());
    }
}
