package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TestTableJdbcRouterTest {
    private static final String PREFIX = "frog2_test_router_contract_";
    private final TestTableJdbcRouter router = new TestTableJdbcRouter(PREFIX);

    @Test
    void routesOnlyApprovedReferencesAndPreservesLiteralsAndBindings() throws Exception {
        String sql = "SELECT d.customer_name FROM vertica_customer_detail d "
                + "LEFT JOIN customer_maintenance_schedule s ON s.customer_name = d.customer_name "
                + "WHERE d.customer_type = 'FROM company_users' AND d.main_manager_user_id = ?";
        String routed = router.routeSql(sql);
        assertTrue(routed.contains("FROM public." + PREFIX + "vertica_customer_detail d"));
        assertTrue(routed.contains("JOIN public." + PREFIX + "customer_maintenance_schedule s"));
        assertTrue(routed.contains("'FROM company_users'"));
        for (String mutation : new String[] {
                "UPDATE\nmaintenance_records SET note = ? WHERE maintenance_id = ?",
                "DELETE\tFROM maintenance_records WHERE maintenance_id = ?",
                "UPDATE maintenance_records SET note = (SELECT userName FROM company_users WHERE userId = ?) "
                        + "WHERE maintenance_id = ?"
        }) {
            assertTrue(router.routeSql(mutation).contains("public." + PREFIX + "maintenance_records"));
        }
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        try (Connection connection = router.wrap(jdbc.open());
                var statement = connection.prepareStatement("DELETE FROM maintenance_records WHERE maintenance_id = ?")) {
            statement.setLong(1, 17L);
            assertEquals(1, statement.executeUpdate());
        }
        assertEquals("DELETE FROM public." + PREFIX + "maintenance_records WHERE maintenance_id = ?",
                jdbc.statements.getFirst().sql);
        assertEquals(17L, jdbc.statements.getFirst().parameters.get(1));
    }

    @Test
    void rejectsUnapprovedTablesAndUnsupportedSqlBeforePreparingAnything() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        try (Connection connection = router.wrap(jdbc.open())) {
            for (String sql : new String[] {
                    "DELETE FROM company_users", "UPDATE company_users SET userName = ?",
                    "UPDATE\nmaintenance_records SET note = ?", "DELETE\tFROM maintenance_records",
                    "UPDATE maintenance_records SET note = "
                            + "(SELECT userName FROM company_users WHERE userId = ?)",
                    "DELETE FROM troubleshooting WHERE troubleshooting_id = ?",
                    "SELECT * FROM public.company_users", "SELECT * FROM company_users, troubleshooting",
                    "SELECT * FROM company_users u JOIN troubleshooting t ON u.userId = t.user_id",
                    "DELETE FROM maintenance_records USING company_users WHERE maintenance_id = ?",
                    "SELECT * FROM company_users UNION SELECT * FROM troubleshooting",
                    "SELECT NEXTVAL('business_sequence') FROM company_users",
                    "SELECT SETVAL('business_sequence', 1) FROM company_users",
                    "SELECT CURRVAL('business_sequence') FROM company_users",
                    "SELECT * FROM company_users; DELETE FROM company_users",
                    "SELECT * FROM company_users -- comment", "SELECT * FROM \"company_users\"",
                    "SELECT * FROM (SELECT * FROM company_users) u",
                    "DROP TABLE company_users", "CREATE TABLE company_users (userId VARCHAR(100))"
            }) {
                assertThrows(SQLException.class, () -> connection.prepareStatement(sql), sql);
            }
            assertEquals(0, jdbc.statements.size());
            assertThrows(SQLException.class, connection::createStatement);
            assertThrows(SQLException.class, () -> connection.prepareCall("CALL anything()"));
            assertThrows(SQLException.class, () -> connection.unwrap(Connection.class));
        }
    }

    @Test
    void preparedStatementCannotEscapeRoutingThroughSqlOrConnectionAccess() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        try (Connection connection = router.wrap(jdbc.open());
                var statement = connection.prepareStatement("SELECT * FROM company_users WHERE userId = ?")) {
            assertThrows(SQLException.class, () -> statement.execute("DELETE FROM company_users"));
            assertThrows(SQLException.class, () -> statement.addBatch("DELETE FROM company_users"));
            assertThrows(SQLException.class, () -> statement.getConnection().createStatement());
            assertThrows(SQLException.class, () -> statement.unwrap(java.sql.PreparedStatement.class));
        }
        assertEquals(1, jdbc.statements.size());
    }

    @Test
    void rejectsImplicitTablesAfterExplicitJoinsWithoutPreparingSql() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        try (Connection connection = router.wrap(jdbc.open())) {
            for (String sql : new String[] {
                    "SELECT u.userId FROM company_users u JOIN maintenance_records r "
                            + "ON r.created_by_user_id = u.userId, troubleshooting t WHERE u.userId = ?",
                    "SELECT u.userId FROM company_users u CROSS JOIN maintenance_records r, troubleshooting t",
                    "SELECT u.userId FROM company_users u JOIN maintenance_records r "
                            + "ON r.customer_name IN (?, ?), company_users other_user WHERE u.userId = ?",
                    "SELECT u.userId FROM company_users u WHERE EXISTS (SELECT r.maintenance_id "
                            + "FROM maintenance_records r JOIN company_users x "
                            + "ON r.created_by_user_id = x.userId, troubleshooting t)",
                    "SELECT u.userId FROM company_users u JOIN "
                            + "(troubleshooting t CROSS JOIN company_users x) ON x.userId = u.userId"
            }) {
                assertThrows(SQLException.class, () -> connection.prepareStatement(sql), sql);
            }
        }
        assertEquals(0, jdbc.statements.size());
    }

    @Test
    void allowsSelectFunctionPredicateAndOrderingCommasForReviewedJoins() throws Exception {
        String sql = "SELECT d.customer_name, COALESCE(d.main_manager, 'FROM unrelated, table') "
                + "FROM vertica_customer_detail d LEFT JOIN customer_maintenance_schedule s "
                + "ON s.customer_name = d.customer_name AND s.interval_months IN (?, ?) "
                + "WHERE d.customer_name IN (?, ?) ORDER BY d.customer_name, d.main_manager";

        String routed = router.routeSql(sql);

        assertTrue(routed.contains("FROM public." + PREFIX + "vertica_customer_detail d"));
        assertTrue(routed.contains("JOIN public." + PREFIX + "customer_maintenance_schedule s"));
        assertTrue(routed.contains("COALESCE(d.main_manager, 'FROM unrelated, table')"));
        assertTrue(routed.contains("ORDER BY d.customer_name, d.main_manager"));
    }

    @Test
    void originalSchemaCapabilitiesReadOnlyThePrefixedTableMetadata() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.main_manager_user_id",
                PREFIX + "vertica_customer_detail.sub_manager_user_id",
                PREFIX + "maintenance_records.created_by_user_id");
        try (Connection connection = router.wrap(publicSchemaMetadata(jdbc.open()))) {
            SchemaCapabilityCache capabilities = new SchemaCapabilityCache();
            assertFalse(capabilities.columnExists(connection, "vertica_customer_detail", "main_manager_user_id"));
            assertTrue(capabilities.columnExists(connection, "vertica_customer_detail", "sub_manager_user_id"));
            assertTrue(capabilities.columnExists(connection, "maintenance_records", "created_by_user_id"));
            assertThrows(SQLException.class,
                    () -> connection.getMetaData().getColumns(null, null, "customer_identity", "%"));
            try (var columns = connection.getMetaData().getColumns(null, null, "maintenance_records", "%")) {
                assertTrue(columns.next());
                assertEquals("maintenance_records", columns.getString("TABLE_NAME"));
            }
        }
    }

    @Test
    void prefixMustIdentifyFourDistinctTestTables() {
        for (String invalid : new String[] {"", "company_", "frog2_test_unsafe;_", "frog2_test_missing_suffix"}) {
            assertThrows(IllegalArgumentException.class, () -> new TestTableJdbcRouter(invalid));
        }
        assertEquals(4, router.tableNames().size());
        assertTrue(router.tableNames().values().stream().allMatch(name -> name.startsWith(PREFIX)));
    }

    private static Connection publicSchemaMetadata(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, call, args) -> {
                    if (!"getMetaData".equals(call.getName())) {
                        try {
                            return call.invoke(delegate, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    }
                    DatabaseMetaData metadata = delegate.getMetaData();
                    return Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(),
                            new Class<?>[] {DatabaseMetaData.class}, (ignored, metadataCall, metadataArgs) -> {
                                assertEquals("public", metadataArgs[1]);
                                ResultSet result = (ResultSet) metadataCall.invoke(metadata, metadataArgs);
                                boolean[] foreignSchemaRow = {false};
                                boolean[] firstRow = {true};
                                return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                                        new Class<?>[] {ResultSet.class}, (rowProxy, resultCall, resultArgs) -> {
                                            if ("next".equals(resultCall.getName())) {
                                                if (firstRow[0]) {
                                                    firstRow[0] = false;
                                                    foreignSchemaRow[0] = true;
                                                    return true;
                                                }
                                                foreignSchemaRow[0] = false;
                                                return result.next();
                                            }
                                            if (foreignSchemaRow[0] && "getString".equals(resultCall.getName())) {
                                                return switch (String.valueOf(resultArgs[0])) {
                                                    case "TABLE_SCHEM" -> "other_schema";
                                                    case "TABLE_NAME" -> metadataArgs[2];
                                                    case "COLUMN_NAME" -> "main_manager_user_id";
                                                    default -> null;
                                                };
                                            }
                                            if ("getString".equals(resultCall.getName())
                                                    && "TABLE_SCHEM".equals(resultArgs[0])) {
                                                return "public";
                                            }
                                            return resultCall.invoke(result, resultArgs);
                                        });
                            });
                });
    }
}
