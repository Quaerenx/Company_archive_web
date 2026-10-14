package com.company.model;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerDetailConcurrencyTest {
    private static final Set<String> ASSIGNEE_COLUMNS = Set.of(
            "vertica_customer_detail.main_manager_user_id", "vertica_customer_detail.sub_manager_user_id");

    @ParameterizedTest
    @ValueSource(strings = {"prod", "stg", "dev"})
    void staleFormDoesNotOverwriteTheLatestDatabaseValues(String environment) {
        Map<String, Object> original = row("old-db", "old-note");
        CustomerDetailDTO submitted = submission(revision(environment, original, false));
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(row("new-db", "old-note"));

        assertThrows(CustomerEditConflictException.class,
                () -> save(new CustomerDetailDAO(jdbc::open), environment, submitted));

        assertEquals(0, jdbc.commitCount);
        assertEquals(1, jdbc.rollbackCount);
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.startsWith("UPDATE")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "stg", "dev"})
    void unchangedOriginalAllowsAnEditAndBindsTheOriginalValuesAtomically(String environment) {
        Map<String, Object> original = row("old-db", "old-note");
        Timestamp installed = Timestamp.valueOf("2026-10-14 09:10:11.123456789");
        original.put("install_date", installed);
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(original);
        jdbc.enqueueUpdate(1);
        CustomerDetailDTO submitted = submission(revision(environment, original, false));

        assertTrue(save(new CustomerDetailDAO(jdbc::open), environment, submitted));

        var mutation = jdbc.statements.getLast();
        assertTrue(mutation.sql.contains("AND customer_name = ?"));
        assertTrue(mutation.sql.contains("db_name = ?"));
        assertTrue(mutation.sql.contains("system_name IS NULL"));
        assertTrue(mutation.parameters.containsValue("old-db"));
        assertTrue(mutation.parameters.containsValue("changed-note"));
        assertTrue(mutation.parameters.containsValue(installed));
        assertEquals(1, jdbc.commitCount);
        assertEquals(0, jdbc.rollbackCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "stg", "dev"})
    void changeBetweenSnapshotReadAndUpdateIsAConflictWithoutAnInsertFallback(String environment) {
        Map<String, Object> original = row("old-db", "old-note");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(original);
        jdbc.enqueueUpdate(0);

        assertThrows(CustomerEditConflictException.class, () -> save(new CustomerDetailDAO(jdbc::open),
                environment, submission(revision(environment, original, false))));

        assertEquals(1, jdbc.rollbackCount);
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.startsWith("INSERT")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"stg", "dev"})
    void missingEnvironmentCanBeCreatedButCannotOverwriteAnEnvironmentCreatedLater(String environment) {
        PaginationJdbcFixture create = new PaginationJdbcFixture();
        create.enqueue();
        create.enqueueUpdate(1);
        assertTrue(save(new CustomerDetailDAO(create::open), environment,
                submission(CustomerDetailDTO.MISSING_EDIT_REVISION)));
        assertEquals(1, create.commitCount);
        assertTrue(create.statements.getLast().sql.startsWith("INSERT"));

        PaginationJdbcFixture raced = new PaginationJdbcFixture();
        raced.enqueue(row("other-db", "other-note"));
        assertThrows(CustomerEditConflictException.class, () -> save(new CustomerDetailDAO(raced::open),
                environment, submission(CustomerDetailDTO.MISSING_EDIT_REVISION)));
        assertEquals(1, raced.rollbackCount);
        assertEquals(0, raced.commitCount);
    }

    @Test
    void unchangedNamesPreserveExistingIdsWithoutANameLookup() {
        Map<String, Object> original = assignedRow("Shared name", "existing-id");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNEE_COLUMNS;
        jdbc.enqueue(original);
        jdbc.enqueueUpdate(1);
        CustomerDetailDTO submitted = submission(revision("prod", original, true));
        submitted.setMainManager("Shared name");

        assertTrue(new CustomerDetailDAO(jdbc::open).saveOrUpdateCustomerDetail(submitted, "actor"));

        var mutation = jdbc.statements.getLast();
        assertEquals("existing-id", mutation.parameters.get(50));
        assertTrue(mutation.sql.contains("main_manager_user_id = ?"));
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.contains("FROM company_users")));
    }

    @Test
    void ambiguousChangedNameIsRejectedBeforeAnyMutation() {
        Map<String, Object> original = assignedRow("Original name", "existing-id");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNEE_COLUMNS;
        jdbc.enqueue(original);
        jdbc.enqueue(PaginationJdbcFixture.row("user_id", "one-of-two", "user_count", 2));
        CustomerDetailDTO submitted = submission(revision("prod", original, true));
        submitted.setMainManager("Shared name");

        assertThrows(CustomerAssignmentException.class,
                () -> new CustomerDetailDAO(jdbc::open).saveOrUpdateCustomerDetail(submitted, "actor"));

        assertEquals(1, jdbc.rollbackCount);
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.startsWith("UPDATE")));
    }

    @Test
    void uniqueChangedNameCanReplaceAnAssignment() {
        Map<String, Object> original = assignedRow("Original name", "existing-id");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNEE_COLUMNS;
        jdbc.enqueue(original);
        jdbc.enqueue(PaginationJdbcFixture.row("user_id", "new-id", "user_count", 1));
        jdbc.enqueueUpdate(1);
        CustomerDetailDTO submitted = submission(revision("prod", original, true));
        submitted.setMainManager("New name");

        assertTrue(new CustomerDetailDAO(jdbc::open).saveOrUpdateCustomerDetail(submitted, "actor"));
        assertEquals("new-id", jdbc.statements.getLast().parameters.get(50));
        assertTrue(jdbc.statements.getLast().parameters.containsValue("existing-id"));
    }

    @Test
    void anIdOnlyAssignmentChangeInvalidatesTheOriginalRevision() {
        Map<String, Object> original = assignedRow("Shared name", "first-id");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNEE_COLUMNS;
        jdbc.enqueue(assignedRow("Shared name", "second-id"));
        CustomerDetailDTO submitted = submission(revision("prod", original, true));
        submitted.setMainManager("Shared name");

        assertThrows(CustomerEditConflictException.class,
                () -> new CustomerDetailDAO(jdbc::open).saveOrUpdateCustomerDetail(submitted, "actor"));
        assertEquals(1, jdbc.rollbackCount);
    }

    @Test
    void revisionsDistinguishNullEmptyAndTimestampNanoseconds() {
        Map<String, Object> first = row("db", "note");
        Map<String, Object> second = row("db", "note");
        second.put("system_name", "");
        assertNotEquals(revision("dev", first, false), revision("dev", second, false));
        first.put("install_date", Timestamp.valueOf("2026-10-14 09:00:00.123456001"));
        second.put("system_name", null);
        second.put("install_date", Timestamp.valueOf("2026-10-14 09:00:00.123456002"));
        assertNotEquals(revision("dev", first, false), revision("dev", second, false));
    }

    @Test
    void basicCustomerUpdateAlsoPreservesAnUnchangedAssignment() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNEE_COLUMNS;
        jdbc.enqueue(assignedRow("Shared name", "existing-id"));
        jdbc.enqueueUpdate(1);
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Acme");
        customer.setManagerName("Shared name");

        assertTrue(new CustomerDAO(jdbc::open).updateCustomer(customer));
        assertEquals("existing-id", jdbc.statements.getLast().parameters.get(20));
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.contains("FROM company_users")));
    }

    @Test
    void uuidRelationshipsArePartOfTheOriginalRevisionAndAtomicPredicate() {
        Set<String> references = Set.of("customer_identity.customer_id", "customer_identity.customer_name",
                "vertica_customer_detail_dev.customer_id");
        String id = "11111111-1111-1111-1111-111111111111";
        Map<String, Object> original = row("db", "note");
        original.put("customer_id", id);
        PaginationJdbcFixture read = new PaginationJdbcFixture();
        read.availableColumns = references;
        read.enqueue(original);
        String revision = new CustomerDetailDAO(read::open).getCustomerDetailDev("Acme").getEditRevision();
        PaginationJdbcFixture write = new PaginationJdbcFixture();
        write.availableColumns = references;
        write.enqueue(original);
        write.enqueue(PaginationJdbcFixture.row("customer_id", id));
        write.enqueueUpdate(1);

        assertTrue(new CustomerDetailDAO(write::open).saveOrUpdateCustomerDetailDev(submission(revision)));
        var update = write.statements.getLast();
        assertTrue(update.sql.contains("AND customer_id = CAST(? AS UUID)"));
        assertEquals(id, update.parameters.get(50));
        assertEquals(id, update.parameters.get(update.parameters.size()));

        Map<String, Object> reassigned = row("db", "note");
        reassigned.put("customer_id", "22222222-2222-2222-2222-222222222222");
        PaginationJdbcFixture race = new PaginationJdbcFixture();
        race.availableColumns = references;
        race.enqueue(reassigned);
        assertThrows(CustomerEditConflictException.class,
                () -> new CustomerDetailDAO(race::open).saveOrUpdateCustomerDetailDev(submission(revision)));
    }

    @Test
    void anInsertRaceRollsBackAndDoesNotRetryOverTheNewEnvironment() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        CustomerDetailDAO dao = new CustomerDetailDAO(() -> duplicateInsertConnection(jdbc.open()));

        assertThrows(CustomerEditConflictException.class,
                () -> dao.saveOrUpdateCustomerDetailDev(submission(CustomerDetailDTO.MISSING_EDIT_REVISION)));

        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.rollbackCount);
        assertEquals(0, jdbc.commitCount);
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.startsWith("UPDATE")));
    }

    private static Connection duplicateInsertConnection(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    try {
                        Object value = method.invoke(delegate, arguments);
                        if (method.getName().equals("prepareStatement")
                                && ((String) arguments[0]).startsWith("INSERT")) {
                            PreparedStatement statement = (PreparedStatement) value;
                            return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                    new Class<?>[]{PreparedStatement.class}, (ignored, call, parameters) -> {
                                        if (call.getName().equals("executeUpdate")) {
                                            throw new SQLException("Concurrent environment insert", "23505");
                                        }
                                        try { return call.invoke(statement, parameters); }
                                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                        return value;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    private static Map<String, Object> row(String database, String note) {
        return PaginationJdbcFixture.row("customer_name", "Acme", "db_name", database, "note", note);
    }

    private static Map<String, Object> assignedRow(String name, String id) {
        Map<String, Object> result = row("old-db", "old-note");
        result.put("main_manager", name);
        result.put("main_manager_user_id", id);
        return result;
    }

    private static CustomerDetailDTO submission(String revision) {
        CustomerDetailDTO result = new CustomerDetailDTO();
        result.setCustomerName("Acme");
        result.setDbName("old-db");
        result.setNote("changed-note");
        result.setEditRevision(revision);
        return result;
    }

    private static String revision(String environment, Map<String, Object> row, boolean assigneeIds) {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = assigneeIds ? ASSIGNEE_COLUMNS : Set.of();
        jdbc.enqueue(row);
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);
        return switch (environment) {
            case "prod" -> dao.getCustomerDetail("Acme").getEditRevision();
            case "stg" -> dao.getCustomerDetailStg("Acme").getEditRevision();
            default -> dao.getCustomerDetailDev("Acme").getEditRevision();
        };
    }

    private static boolean save(CustomerDetailDAO dao, String environment, CustomerDetailDTO detail) {
        return switch (environment) {
            case "prod" -> dao.saveOrUpdateCustomerDetail(detail, "actor");
            case "stg" -> dao.saveOrUpdateCustomerDetailStg(detail);
            default -> dao.saveOrUpdateCustomerDetailDev(detail);
        };
    }
}
