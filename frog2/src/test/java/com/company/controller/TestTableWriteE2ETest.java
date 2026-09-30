package com.company.controller;

import static com.company.controller.IsolatedAuthenticatedWriteFlowTest.filtered;
import static com.company.controller.IsolatedAuthenticatedWriteFlowTest.maintenanceRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.controller.IsolatedAuthenticatedWriteFlowTest.Request;
import com.company.controller.IsolatedAuthenticatedWriteFlowTest.Session;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.TestTableJdbcRouter;
import com.company.security.CsrfToken;
import com.company.util.BusinessDate;
import com.company.util.PasswordUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real JDBC and production servlet/filter flow, restricted to approved empty test tables. */
@Tag("e2e-table-write")
class TestTableWriteE2ETest {
    private static final String CUSTOMER = "Isolated Customer";
    private static final String OTHER_CUSTOMER = "Other Isolated Customer";
    private static final String OWNER = "table-test-owner";
    private static final String SECONDARY = "table-test-secondary";
    private static final String ATTACKER = "table-test-attacker";
    private static final List<String> TABLES = List.of(
            "maintenance_records", "customer_maintenance_schedule", "vertica_customer_detail", "company_users");

    @Test
    void realDaoAuthenticationAssignmentsAndFilteredCrudStayInsideTestTables() throws Exception {
        try {
            runProbe();
        } catch (Exception failure) {
            String state = "unknown";
            Throwable current = failure;
            for (int depth = 0; current != null && depth < 20; depth++, current = current.getCause()) {
                if (current instanceof java.sql.SQLException sql && sql.getSQLState() != null
                        && sql.getSQLState().matches("[A-Z0-9]{5}")) {
                    state = sql.getSQLState();
                    break;
                }
            }
            throw new AssertionError("Test-table write probe failed: type="
                    + failure.getClass().getSimpleName() + ", SQLState=" + state);
        }
    }

    private void runProbe() throws Exception {
        assertEquals("true", required("FROG2_TABLE_WRITE_ENABLED"));
        String prefix = required("FROG2_TABLE_WRITE_PREFIX");
        TestTableJdbcRouter router = new TestTableJdbcRouter(prefix);
        Properties properties = databaseProperties();
        Class.forName(properties.getProperty("db.driver"));
        DriverManager.setLoginTimeout(5);
        TestTableJdbcRouter.ConnectionOpener opener = () -> {
            Connection connection = DriverManager.getConnection(properties.getProperty("db.url"),
                    properties.getProperty("db.user"), properties.getProperty("db.password"));
            try {
                connection.setNetworkTimeout(Runnable::run, 5000);
                return connection;
            } catch (java.sql.SQLException failure) {
                connection.close();
                throw failure;
            }
        };
        var daos = router.daos(opener);
        try (Connection connection = router.wrap(opener.open())) {
            verifyEmptyTables(connection);
            verifyIndependentDefaults(connection, prefix);
            String password = "Table-fixture-" + UUID.randomUUID() + "-Aa9!";
            try {
                seed(connection, password);
                assertNotNull(daos.users().authenticateUser(OWNER, password));
                assertNull(daos.users().authenticateUser(OWNER, "invalid-password"));
                assertEquals(Set.of(CUSTOMER), daos.assignments().getCustomerNamesByAssignee(OWNER, "Same Name"));
                assertEquals(Set.of(CUSTOMER), daos.assignments().getCustomerNamesByAssignee(SECONDARY, "Same Name"));
                assertEquals(Set.of(OTHER_CUSTOMER), daos.assignments().getCustomerNamesByAssignee(ATTACKER, "Same Name"));
                var personal = daos.assignments().getMaintenanceAssigneeData(SECONDARY, "Same Name");
                assertEquals(List.of(CUSTOMER), personal.customers().stream().map(customer -> customer.getCustomerName()).toList());
                assertTrue(personal.assignments().getFirst().schedule().isDue(java.time.YearMonth.of(2026, 9)));

                MaintenanceServlet servlet = new MaintenanceServlet(
                        daos.records(), daos.customers(), daos.assignments(), BusinessDate.systemClock());
                Session owner = new Session(daos.users().authenticateUser(OWNER, password));
                Request create = maintenanceRequest(owner, "add", "table-created");
                create.parameters.put("created_by_user_id", ATTACKER);
                assertEquals(302, filtered(create, servlet::doPost).status);
                MaintenanceRecordDTO persisted = onlyRecord(daos);
                assertEquals(OWNER, persisted.getCreatorUserId());
                assertEquals("table-created", persisted.getNote());
                assertEquals("25.4.0-9", persisted.getVerticaVersion());
                assertEquals("2", persisted.getLicenseSizeGb());
                long id = persisted.getMaintenanceId();

                Session attacker = new Session(daos.users().authenticateUser(ATTACKER, password));
                for (String action : List.of("update", "delete")) {
                    Request denied = forRecord(attacker, action, "attacker-edit", id);
                    assertEquals(302, filtered(denied, servlet::doPost).status);
                    assertEquals("table-created", daos.records().getMaintenanceRecordById(id).getNote());
                }
                Request badCsrf = forRecord(owner, "update", "invalid-csrf", id);
                badCsrf.headers.put(CsrfToken.HEADER_NAME, attacker.csrf);
                assertEquals(403, filtered(badCsrf, servlet::doPost).status);
                assertEquals("table-created", daos.records().getMaintenanceRecordById(id).getNote());

                Session secondary = new Session(daos.users().authenticateUser(SECONDARY, password));
                assertEquals(302, filtered(forRecord(secondary, "update", "secondary-edited", id), servlet::doPost).status);
                MaintenanceRecordDTO edited = daos.records().getMaintenanceRecordById(id);
                assertEquals("secondary-edited", edited.getNote());
                assertEquals(OWNER, edited.getCreatorUserId());
                assertFalse(daos.records().deleteMaintenanceRecordForCustomer(id, OTHER_CUSTOMER));
                assertNotNull(daos.records().getMaintenanceRecordById(id));

                execute(connection, "UPDATE vertica_customer_detail SET main_manager_user_id = ? WHERE customer_name = ?",
                        null, CUSTOMER);
                assertEquals(302, filtered(forRecord(owner, "delete", "", id), servlet::doPost).status);
                assertNotNull(daos.records().getMaintenanceRecordById(id));
                assertEquals(302, filtered(forRecord(secondary, "delete", "", id), servlet::doPost).status);
                assertNull(daos.records().getMaintenanceRecordById(id));
            } finally {
                cleanupAndVerify(connection);
            }
        }
        System.out.println("TABLE_WRITE_PROBE|tables=4|authentication=passed|csrf=passed|assignmentAuthorization=passed|crud=passed|cleanup=empty");
    }

    private static MaintenanceRecordDTO onlyRecord(TestTableJdbcRouter.DaoSet daos) {
        var records = daos.records().getMaintenanceRecordsByMonthForCustomers(
                Date.valueOf("2026-09-01"), Date.valueOf("2026-10-01"), List.of(CUSTOMER));
        assertEquals(1, records.size());
        MaintenanceRecordDTO persisted = daos.records().getMaintenanceRecordById(
                records.getFirst().getMaintenanceId());
        assertNotNull(persisted);
        return persisted;
    }

    private static Request forRecord(Session session, String action, String note, long id) {
        Request request = maintenanceRequest(session, action, note);
        request.parameters.put("maintenance_id", Long.toString(id));
        return request;
    }

    private static void seed(Connection connection, String password) throws Exception {
        String hash = PasswordUtils.hashPassword(password);
        for (String user : List.of(OWNER, SECONDARY, ATTACKER)) {
            execute(connection, "INSERT INTO company_users (userId, password, userName) VALUES (?, ?, ?)", user, hash, "Same Name");
        }
        for (String customer : List.of(CUSTOMER, OTHER_CUSTOMER)) {
            boolean own = CUSTOMER.equals(customer);
            execute(connection, "INSERT INTO vertica_customer_detail "
                            + "(customer_name, main_manager, sub_manager, main_manager_user_id, sub_manager_user_id, "
                            + "customer_type, is_deleted, vertica_version, license_info) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    customer, "Same Name", own ? "Same Name" : null, own ? OWNER : ATTACKER, own ? SECONDARY : null,
                    "정기점검 계약 고객사", 1, "25.4.0-9", "2TB");
            execute(connection, "INSERT INTO customer_maintenance_schedule "
                            + "(customer_name, interval_months, anchor_month, effective_from, enabled, updated_by) VALUES (?, ?, ?, ?, ?, ?)",
                    customer, 1, Date.valueOf("2026-09-01"), Date.valueOf("2026-09-01"), true, OWNER);
        }
    }

    static void cleanupAndVerify(Connection connection) throws Exception {
        Throwable failure = null;
        try {
            cleanup(connection);
        } catch (Exception | AssertionError exception) {
            failure = exception;
        }
        try {
            verifyEmptyTables(connection);
        } catch (Exception | AssertionError exception) {
            failure = appendFailure(failure, exception);
        }
        throwFailure(failure);
    }

    private static void cleanup(Connection connection) throws Exception {
        Throwable failure = null;
        for (String table : TABLES) {
            try {
                if ("company_users".equals(table)) {
                    execute(connection, "DELETE FROM company_users WHERE userId IN (?, ?, ?)", OWNER, SECONDARY, ATTACKER);
                } else {
                    execute(connection, "DELETE FROM " + table + " WHERE customer_name IN (?, ?)", CUSTOMER, OTHER_CUSTOMER);
                }
            } catch (Exception | AssertionError exception) {
                failure = appendFailure(failure, exception);
            }
        }
        throwFailure(failure);
    }

    private static void verifyEmptyTables(Connection connection) throws Exception {
        Throwable failure = null;
        for (String table : TABLES) {
            try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                    var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(0, rows.getLong(1), "Approved test table must be empty: " + table);
            } catch (Exception | AssertionError exception) {
                failure = appendFailure(failure, exception);
            }
        }
        throwFailure(failure);
    }

    private static Throwable appendFailure(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        first.addSuppressed(next);
        return first;
    }

    private static void throwFailure(Throwable failure) throws Exception {
        if (failure instanceof Exception exception) {
            throw exception;
        }
        if (failure instanceof AssertionError assertion) {
            throw assertion;
        }
    }

    private static void verifyIndependentDefaults(Connection connection, String prefix) throws Exception {
        boolean independentMaintenanceId = false;
        for (String table : TABLES) {
            try (var columns = connection.getMetaData().getColumns(null, null, table, "%")) {
                boolean present = false;
                while (columns.next()) {
                    present = true;
                    String value = columns.getString("COLUMN_DEF");
                    if (value != null && value.toLowerCase(Locale.ROOT).contains("nextval")) {
                        assertTrue(value.contains(prefix), "Sequence defaults must reference the approved test prefix");
                        if ("maintenance_records".equals(table)
                                && "maintenance_id".equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                            independentMaintenanceId = true;
                        }
                    }
                }
                assertTrue(present, "Approved test table metadata is required: " + table);
            }
        }
        assertTrue(independentMaintenanceId, "The test maintenance ID must use an independent test sequence");
    }

    private static void execute(Connection connection, String sql, Object... values) throws Exception {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                if (values[index] == null) {
                    statement.setNull(index + 1, java.sql.Types.VARCHAR);
                } else {
                    statement.setObject(index + 1, values[index]);
                }
            }
            assertTrue(statement.executeUpdate() >= 0);
        }
    }

    private static Properties databaseProperties() throws Exception {
        Path config = Path.of(required("FROG2_TABLE_WRITE_DB_CONFIG")).toRealPath();
        if (!Files.isRegularFile(config)) {
            throw new IllegalArgumentException("An explicit regular external DB config is required");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
        }
        for (String key : List.of("db.driver", "db.url", "db.user", "db.password")) {
            if (properties.getProperty(key) == null || properties.getProperty(key).isBlank()) {
                throw new IllegalArgumentException("Required external database config key is missing");
            }
        }
        return properties;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
