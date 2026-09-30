package com.company.e2e;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.TestTableJdbcRouter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthenticatedMaintenanceFixtureTest {
    private static final String PREFIX = "frog2_test_http_fixture_";
    private static final String CUSTOMER = "E2E-synthetic";
    private static final String OWNER = "e2e-owner";

    @Test
    void seededCustomerIsAnActiveMaintenanceAssignmentForTheOwnerOnly() throws Exception {
        JdbcFixture fixture = new JdbcFixture();
        var assignments = new TestTableJdbcRouter(PREFIX).daos(fixture::connection).assignments();
        assertTrue(assignments.getMaintenanceCustomersByAssignee(OWNER, "Same Name").isEmpty());

        AuthenticatedMaintenanceE2ETest.insertMaintenanceCustomer(
                fixture.connection(), CUSTOMER, OWNER, "Same Name");

        var customers = assignments.getMaintenanceCustomersByAssignee(OWNER, "Unrelated Display Name");
        assertEquals(1, customers.size());
        assertEquals(CUSTOMER, customers.getFirst().getCustomerName());
        assertEquals("Same Name", customers.getFirst().getManagerName());
        assertEquals("1TB", customers.getFirst().getLicenseSize());
        assertTrue(assignments.getMaintenanceCustomersByAssignee("e2e-attacker", "Same Name").isEmpty());
    }

    @Test
    void cleanupStillRemovesTheFixtureCustomerAndUsersAfterRecordCleanupFails() throws Exception {
        JdbcFixture fixture = new JdbcFixture();
        AuthenticatedMaintenanceE2ETest.insertMaintenanceCustomer(
                fixture.connection(), CUSTOMER, OWNER, "Same Name");
        fixture.customers.add(Map.of("customer_name", "E2E-unrelated", "main_manager_user_id", OWNER));
        fixture.failRecordCleanup = true;

        SQLException failure = assertThrows(SQLException.class,
                () -> AuthenticatedMaintenanceE2ETest.cleanupTemporaryData(
                        fixture.connection(), CUSTOMER, OWNER, "e2e-attacker"));

        assertEquals("Simulated record cleanup failure", failure.getMessage());
        assertEquals(List.of("maintenance_records", "vertica_customer_detail", "company_users"),
                fixture.deletedTables);
        assertEquals(List.of("E2E-unrelated"), fixture.customers.stream()
                .map(row -> row.get("customer_name")).toList());
        assertEquals(Map.of(1, OWNER, 2, "e2e-attacker"), fixture.deletedUsers);
    }

    private static final class JdbcFixture {
        private final List<Map<String, Object>> customers = new ArrayList<>();
        private final List<String> deletedTables = new ArrayList<>();
        private Map<Integer, Object> deletedUsers;
        private boolean failRecordCleanup;

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> statement((String) args[0]);
                        case "getMetaData" -> metadata();
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement statement(String sql) {
            Map<Integer, Object> parameters = new HashMap<>();
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setString", "setInt", "setObject" -> parameters.put((Integer) args[0], args[1]);
                            case "setNull" -> parameters.put((Integer) args[0], null);
                            case "executeUpdate" -> {
                                if (sql.startsWith("INSERT INTO vertica_customer_detail ")) {
                                    String[] columns = sql.substring(sql.indexOf('(') + 1, sql.indexOf(')')).split(",");
                                    Map<String, Object> row = new HashMap<>();
                                    for (int index = 0; index < columns.length; index++) {
                                        row.put(columns[index].trim(), parameters.get(index + 1));
                                    }
                                    customers.add(row);
                                } else if (sql.startsWith("DELETE FROM maintenance_records ")) {
                                    deletedTables.add("maintenance_records");
                                    if (failRecordCleanup) {
                                        throw new SQLException("Simulated record cleanup failure");
                                    }
                                } else if (sql.startsWith("DELETE FROM vertica_customer_detail ")) {
                                    deletedTables.add("vertica_customer_detail");
                                    assertTrue(sql.contains("customer_name = ? AND main_manager_user_id = ?"));
                                    customers.removeIf(row -> parameters.get(1).equals(row.get("customer_name"))
                                            && parameters.get(2).equals(row.get("main_manager_user_id")));
                                } else if (sql.startsWith("DELETE FROM company_users ")) {
                                    deletedTables.add("company_users");
                                    assertTrue(sql.contains("WHERE userId IN (?, ?)"));
                                    deletedUsers = Map.copyOf(parameters);
                                } else {
                                    throw new AssertionError("Unexpected fixture mutation");
                                }
                                return 1;
                            }
                            case "executeQuery" -> {
                                assertTrue(sql.contains("FROM public." + PREFIX + "vertica_customer_detail d"));
                                assertTrue(sql.contains("d.is_deleted = 1"));
                                assertTrue(sql.contains("d.customer_type = ?"));
                                assertTrue(sql.contains("d.main_manager_user_id = ? OR d.sub_manager_user_id = ?"));
                                return rows(customers.stream()
                                        .filter(row -> Integer.valueOf(1).equals(row.get("is_deleted")))
                                        .filter(row -> parameters.get(1).equals(row.get("customer_type")))
                                        .filter(row -> parameters.get(2).equals(row.get("main_manager_user_id"))
                                                || parameters.get(3).equals(row.get("sub_manager_user_id")))
                                        .toList());
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                        return null;
                    });
        }

        private DatabaseMetaData metadata() {
            return (DatabaseMetaData) Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(),
                    new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> {
                        assertEquals("getColumns", method.getName());
                        assertEquals("public", args[1]);
                        assertEquals(PREFIX + "vertica_customer_detail", args[2]);
                        return rows(List.of("main_manager_user_id", "sub_manager_user_id").stream()
                                .filter(column -> "%".equals(args[3]) || column.equals(args[3]))
                                .map(column -> Map.<String, Object>of("TABLE_NAME", args[2],
                                        "TABLE_SCHEM", "public", "COLUMN_NAME", column))
                                .toList());
                    });
        }

        private ResultSet rows(List<Map<String, Object>> values) {
            int[] position = {-1};
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                    new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
                        if ("next".equals(method.getName())) {
                            return ++position[0] < values.size();
                        }
                        if ("getString".equals(method.getName())) {
                            Object value = values.get(position[0]).get(args[0]);
                            return value == null ? null : value.toString();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }
    }
}
