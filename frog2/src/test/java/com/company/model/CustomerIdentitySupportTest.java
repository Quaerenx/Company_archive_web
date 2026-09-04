package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerIdentitySupportTest {
    private static final String CUSTOMER_ID =
            "2d90df87-baca-4ae5-a0c0-d2d135696eb2";

    @Test
    void customerCreateReusesTheImmutableIdentityInOneTransaction() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "customer_identity.customer_id",
                "customer_identity.customer_name");
        jdbc.enqueue(PaginationJdbcFixture.row("customer_id", CUSTOMER_ID));
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Acme");
        assertTrue(dao.addCustomer(customer));

        assertEquals(CUSTOMER_ID, customer.getCustomerId());
        assertEquals(1, jdbc.commitCount);
        assertEquals(0, jdbc.rollbackCount);
        assertEquals(List.of(false, true), jdbc.autoCommitValues);
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "FROM customer_identity WHERE customer_name = ?"));
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "INSERT INTO vertica_customer_detail"));
    }

    @Test
    void customerCreateGeneratesIdentityWhenOneDoesNotExist() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "customer_identity.customer_id",
                "customer_identity.customer_name");
        jdbc.enqueue();
        jdbc.enqueueUpdate(1);
        jdbc.enqueue(PaginationJdbcFixture.row("customer_id", CUSTOMER_ID));
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("New Customer");
        assertTrue(dao.addCustomer(customer));

        assertEquals(CUSTOMER_ID, customer.getCustomerId());
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "INSERT INTO customer_identity"));
        assertTrue(jdbc.statements.get(3).sql.startsWith(
                "INSERT INTO vertica_customer_detail"));
        assertEquals(1, jdbc.commitCount);
    }

    @Test
    void partialIdentitySchemaFailsClosed() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("customer_identity.customer_id");
        CustomerIdentitySupport.Capability capability =
                CustomerIdentitySupport.capability(
                        jdbc.open(), new SchemaCapabilityCache());

        assertThrows(
                SQLException.class,
                () -> CustomerIdentitySupport.requireCompatible(capability));
    }
}
