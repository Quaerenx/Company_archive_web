package com.company.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CustomerDAOSoftDeleteContractTest {
    private static final Pattern RAW_SOFT_DELETE_LITERAL = Pattern.compile(
            "is_deleted\\s*=\\s*[01]");

    @Test
    void namesTheLegacyActiveAndDeletedFlagSemantics() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/company/model/CustomerDAO.java"));

        assertTrue(source.contains(
                "private static final int ACTIVE_FLAG = 1;"));
        assertTrue(source.contains(
                "private static final int DELETED_FLAG = 0;"));
        assertFalse(RAW_SOFT_DELETE_LITERAL.matcher(source).find());
    }

    @Test
    void readUpdateInsertAndDeleteKeepTheExistingFlagContract() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Acme");

        dao.getCustomerByName("Acme");
        assertTrue(dao.updateCustomer(customer));
        assertTrue(dao.addCustomer(customer));
        assertTrue(dao.deleteCustomer("Acme"));

        assertTrue(jdbc.statements.get(0).sql.contains(
                "d.is_deleted = 1"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "is_deleted = 1"));
        assertTrue(jdbc.statements.get(2).sql.endsWith(
                "?, 1)"));
        assertTrue(jdbc.statements.get(3).sql.contains(
                "SET is_deleted = 0"));
        assertTrue(jdbc.statements.get(3).sql.contains(
                "AND is_deleted = 1"));
    }
}
