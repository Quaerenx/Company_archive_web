package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLNonTransientException;
import org.junit.jupiter.api.Test;

class MonthlyCustomerResponseDAOContractTest {
    @Test
    void readOnlyWriteFailureRemainsExplicit() {
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(() -> {
            throw new SQLNonTransientException("blocked", "25006");
        });

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> dao.addResponse(new MonthlyCustomerResponseDTO()));

        assertTrue(exception.isReadOnlyViolation());
        assertEquals(DataAccessException.Kind.READ_ONLY, exception.getKind());
    }
}
