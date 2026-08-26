package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerAuditTrailTest {
    private static final Set<String> AUDIT_COLUMNS = Set.of(
            "vertica_customer_detail.updated_at",
            "vertica_customer_detail.updated_by",
            "vertica_customer_detail.deleted_at",
            "vertica_customer_detail.deleted_by");

    @Test
    void masterMutationsBindTheStableSessionActorWhenAuditColumnsExist() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = AUDIT_COLUMNS;
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(
                jdbc::open, new SchemaCapabilityCache());
        CustomerDTO customer = customer("Acme");

        assertTrue(dao.updateCustomer(customer, " user-17 "));
        assertTrue(dao.addCustomer(customer, "user-17"));
        assertTrue(dao.deleteCustomer("Acme", "user-17"));

        PaginationJdbcFixture.StatementRecord update = jdbc.statements.get(0);
        assertTrue(update.sql.contains(
                "updated_at = CURRENT_TIMESTAMP, updated_by = ?"));
        assertEquals("user-17", update.parameters.get(11));
        assertEquals("Acme", update.parameters.get(12));

        PaginationJdbcFixture.StatementRecord insert = jdbc.statements.get(1);
        assertTrue(insert.sql.contains("updated_at, updated_by"));
        assertTrue(insert.sql.contains("CURRENT_TIMESTAMP, ?"));
        assertEquals("user-17", insert.parameters.get(12));

        PaginationJdbcFixture.StatementRecord delete = jdbc.statements.get(2);
        assertTrue(delete.sql.contains(
                "deleted_at = CURRENT_TIMESTAMP, deleted_by = ?"));
        assertTrue(delete.sql.contains(
                "updated_at = CURRENT_TIMESTAMP, updated_by = ?"));
        assertEquals("user-17", delete.parameters.get(1));
        assertEquals("user-17", delete.parameters.get(2));
        assertEquals("Acme", delete.parameters.get(3));
    }

    @Test
    void legacySchemaKeepsThePreviousMutationSqlEvenWithAnActor() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(
                jdbc::open, new SchemaCapabilityCache());

        assertTrue(dao.updateCustomer(customer("Acme"), "user-17"));

        PaginationJdbcFixture.StatementRecord update = jdbc.statements.getFirst();
        assertFalse(update.sql.contains("updated_at"));
        assertFalse(update.sql.contains("updated_by"));
        assertEquals("Acme", update.parameters.get(11));
    }

    @Test
    void legacyUpdatedAtAloneKeepsThePreviousMutationSql() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.updated_at");
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(
                jdbc::open, new SchemaCapabilityCache());

        assertTrue(dao.updateCustomer(customer("Acme"), "user-17"));

        PaginationJdbcFixture.StatementRecord update =
                jdbc.statements.getFirst();
        assertFalse(update.sql.contains("updated_at = CURRENT_TIMESTAMP"));
        assertFalse(update.sql.contains("updated_by"));
        assertEquals("Acme", update.parameters.get(11));
    }

    @Test
    void partiallyAppliedAuditSchemaFailsClosedForReadsAndWrites() {
        PaginationJdbcFixture masterJdbc = new PaginationJdbcFixture();
        masterJdbc.availableColumns = Set.of(
                "vertica_customer_detail.updated_at",
                "vertica_customer_detail.updated_by");
        CustomerDAO masterDao = new CustomerDAO(
                masterJdbc::open, new SchemaCapabilityCache());

        IllegalStateException masterFailure = assertThrows(
                IllegalStateException.class,
                () -> masterDao.updateCustomer(customer("Acme"), "user-17"));

        assertTrue(masterFailure.getMessage().contains("partially applied"));
        assertTrue(masterJdbc.statements.isEmpty());

        PaginationJdbcFixture detailJdbc = new PaginationJdbcFixture();
        detailJdbc.availableColumns = Set.of(
                "vertica_customer_detail.updated_at",
                "vertica_customer_detail.updated_by",
                "vertica_customer_detail.deleted_at");
        CustomerDetailDAO detailDao = new CustomerDetailDAO(
                detailJdbc::open, new SchemaCapabilityCache());
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("Acme");

        assertThrows(
                IllegalStateException.class,
                () -> detailDao.saveOrUpdateCustomerDetail(detail, "user-17"));
        assertThrows(
                IllegalStateException.class,
                () -> detailDao.getCustomerDetails("Acme"));
        assertTrue(detailJdbc.statements.isEmpty());
    }

    @Test
    void completeAuditSchemaRejectsWritesWithoutAStableActor() {
        PaginationJdbcFixture masterJdbc = new PaginationJdbcFixture();
        masterJdbc.availableColumns = AUDIT_COLUMNS;
        CustomerDAO masterDao = new CustomerDAO(
                masterJdbc::open, new SchemaCapabilityCache());

        IllegalStateException updateFailure = assertThrows(
                IllegalStateException.class,
                () -> masterDao.updateCustomer(customer("Acme")));
        assertThrows(
                IllegalStateException.class,
                () -> masterDao.addCustomer(customer("Acme"), "  "));
        assertThrows(
                IllegalStateException.class,
                () -> masterDao.deleteCustomer("Acme", null));

        assertTrue(updateFailure.getMessage().contains("actor is required"));
        assertTrue(masterJdbc.statements.isEmpty());

        PaginationJdbcFixture detailJdbc = new PaginationJdbcFixture();
        detailJdbc.availableColumns = AUDIT_COLUMNS;
        CustomerDetailDAO detailDao = new CustomerDetailDAO(
                detailJdbc::open, new SchemaCapabilityCache());
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("Acme");

        assertThrows(
                IllegalStateException.class,
                () -> detailDao.saveOrUpdateCustomerDetail(detail));
        assertTrue(detailJdbc.statements.isEmpty());
    }

    @Test
    void productionDetailWriteAndReadUseAuditOnlyWhenCapabilityExists() {
        PaginationJdbcFixture writeJdbc = new PaginationJdbcFixture();
        writeJdbc.availableColumns = AUDIT_COLUMNS;
        writeJdbc.enqueue(PaginationJdbcFixture.row("exists", 1));
        writeJdbc.enqueueUpdate(1);
        CustomerDetailDAO writeDao = new CustomerDetailDAO(
                writeJdbc::open, new SchemaCapabilityCache());
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("Acme");

        assertTrue(writeDao.saveOrUpdateCustomerDetail(detail, "user-17"));

        PaginationJdbcFixture.StatementRecord update = writeJdbc.statements.get(1);
        assertTrue(update.sql.contains(
                "updated_at = CURRENT_TIMESTAMP, updated_by = ?"));
        assertEquals("user-17", update.parameters.get(49));
        assertEquals("Acme", update.parameters.get(50));

        Timestamp modifiedAt = Timestamp.valueOf("2026-08-25 09:30:00");
        PaginationJdbcFixture readJdbc = new PaginationJdbcFixture();
        readJdbc.availableColumns = AUDIT_COLUMNS;
        readJdbc.enqueue(PaginationJdbcFixture.row(
                "detail_environment", "prod",
                "customer_name", "Acme",
                "audit_updated_at", modifiedAt,
                "audit_updated_by", "user-17"));
        CustomerDetailDAO readDao = new CustomerDetailDAO(
                readJdbc::open, new SchemaCapabilityCache());

        CustomerDetailDTO production =
                readDao.getCustomerDetails("Acme").production();

        assertEquals(modifiedAt, new Timestamp(
                production.getUpdatedAt().getTime()));
        assertEquals("user-17", production.getUpdatedBy());
        assertTrue(readJdbc.statements.getFirst().sql.contains(
                "updated_at AS audit_updated_at"));
    }

    @Test
    void legacyDetailReadReturnsNoAuditMetadata() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "detail_environment", "prod",
                "customer_name", "Acme"));
        CustomerDetailDAO dao = new CustomerDetailDAO(
                jdbc::open, new SchemaCapabilityCache());

        CustomerDetailDTO production = dao.getCustomerDetails("Acme").production();

        assertNull(production.getUpdatedAt());
        assertNull(production.getUpdatedBy());
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "CAST(NULL AS TIMESTAMP) AS audit_updated_at"));
    }

    @Test
    void detailViewHidesAuditMetadataUnlessTheDaoProvidesIt() throws Exception {
        String view = Files.readString(Path.of(
                "src/main/webapp/customers/customers_detail.jsp"));

        assertTrue(view.contains(
                "not empty customerDetail.updatedAt or not empty customerDetail.updatedBy"));
        assertTrue(view.contains("마지막 수정"));
        assertTrue(view.contains("${customerDetail.updatedBy}"));
        assertTrue(view.contains("${customerDetail.updatedAt}"));
    }

    @Test
    void migrationIsIdempotentAndDoesNotInventHistoricalAuditValues() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/"
                        + "V20260825_09__add_customer_audit_columns.sql"));

        assertEquals(4, count(migration, "ADD COLUMN IF NOT EXISTS"));
        assertTrue(migration.contains("updated_at TIMESTAMP"));
        assertTrue(migration.contains("updated_by VARCHAR(100)"));
        assertTrue(migration.contains("deleted_at TIMESTAMP"));
        assertTrue(migration.contains("deleted_by VARCHAR(100)"));
        assertFalse(migration.matches("(?is).*\\bUPDATE\\s+vertica_customer_detail\\b.*"));
    }

    private static int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static CustomerDTO customer(String name) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        return customer;
    }
}
