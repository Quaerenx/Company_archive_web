package com.company.model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerTransactionFailureTest {
    @Test
    void detailRollbackFailureKeepsTheWriteFailureAndNeverRestoresAutoCommit() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        SQLException writeFailure = new SQLException("Synthetic write failure", "25006");
        SQLException rollbackFailure = new SQLException("Synthetic rollback failure", "08006");
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("Synthetic customer");

        DataAccessException failure = assertThrows(DataAccessException.class,
                () -> new CustomerDetailDAO(() -> failingConnection(jdbc.open(), writeFailure, rollbackFailure))
                        .saveOrUpdateCustomerDetailDev(detail));

        assertSame(writeFailure, failure.getCause());
        assertTrue(failure.isReadOnlyViolation());
        assertEquals(List.of(rollbackFailure), List.of(writeFailure.getSuppressed()));
        assertEquals(List.of(false), jdbc.autoCommitValues);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void masterRollbackFailureKeepsTheWriteFailureAndNeverRestoresAutoCommit() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("customer_identity.customer_id", "customer_identity.customer_name");
        jdbc.enqueue(PaginationJdbcFixture.row("customer_id", "11111111-1111-1111-1111-111111111111"));
        SQLException writeFailure = new SQLException("Synthetic write failure", "25006");
        SQLException rollbackFailure = new SQLException("Synthetic rollback failure", "08006");
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Synthetic customer");

        DataAccessException failure = assertThrows(DataAccessException.class,
                () -> new CustomerDAO(() -> failingConnection(jdbc.open(), writeFailure, rollbackFailure))
                        .addCustomer(customer));

        assertSame(writeFailure, failure.getCause());
        assertTrue(failure.isReadOnlyViolation());
        assertEquals(List.of(rollbackFailure), List.of(writeFailure.getSuppressed()));
        assertEquals(List.of(false), jdbc.autoCommitValues);
        assertEquals(1, jdbc.closeCount);
    }

    private static Connection failingConnection(Connection delegate, SQLException writeFailure,
            SQLException rollbackFailure) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (ignored, method, args) -> {
                    if (method.getName().equals("rollback")) throw rollbackFailure;
                    try {
                        Object value = method.invoke(delegate, args);
                        if (method.getName().equals("prepareStatement")) {
                            PreparedStatement statement = (PreparedStatement) value;
                            return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                    new Class<?>[] {PreparedStatement.class}, (proxy, call, parameters) -> {
                                        if (call.getName().equals("executeUpdate")) throw writeFailure;
                                        try { return call.invoke(statement, parameters); }
                                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                        return value;
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }
}
