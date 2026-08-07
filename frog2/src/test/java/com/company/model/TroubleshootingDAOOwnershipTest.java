package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TroubleshootingDAOOwnershipTest {
    @Test
    void addBindsStableOwnerIdAndDisplayName() {
        FakeJdbc jdbc = new FakeJdbc(true);
        TroubleshootingDAO dao =
                new TroubleshootingDAO(
                        jdbc::open, new SchemaCapabilityCache());
        TroubleshootingDTO troubleshooting = troubleshooting();

        assertTrue(dao.addTroubleshooting(troubleshooting));

        StatementRecord insert = jdbc.statements.getFirst();
        assertTrue(insert.sql.contains(
                "work_period, creator_user_id, creator, support_type"));
        assertEquals("owner-1", insert.parameters.get(7));
        assertEquals("Same Name", insert.parameters.get(8));
    }

    @Test
    void updateAndDeleteScopeTheMutationToTheSessionOwnerId() {
        FakeJdbc jdbc = new FakeJdbc(true);
        TroubleshootingDAO dao =
                new TroubleshootingDAO(
                        jdbc::open, new SchemaCapabilityCache());
        TroubleshootingDTO troubleshooting = troubleshooting();

        assertTrue(dao.updateTroubleshootingForOwner(
                troubleshooting, "owner-1"));
        assertTrue(dao.deleteTroubleshootingForOwner(7, "owner-1"));

        StatementRecord update = jdbc.statements.get(0);
        assertTrue(update.sql.endsWith(
                "WHERE id = ? AND creator_user_id = ?"));
        assertEquals(7, update.parameters.get(15));
        assertEquals("owner-1", update.parameters.get(16));

        StatementRecord delete = jdbc.statements.get(1);
        assertEquals(
                "DELETE FROM troubleshooting "
                        + "WHERE id = ? AND creator_user_id = ?",
                delete.sql);
        assertEquals(7, delete.parameters.get(1));
        assertEquals("owner-1", delete.parameters.get(2));
    }

    @Test
    void missingOwnershipColumnFailsClosedBeforePreparingAnyMutation() {
        FakeJdbc jdbc = new FakeJdbc(false);
        TroubleshootingDAO dao =
                new TroubleshootingDAO(
                        jdbc::open, new SchemaCapabilityCache());

        assertFalse(dao.addTroubleshooting(troubleshooting()));
        assertFalse(dao.updateTroubleshootingForOwner(
                troubleshooting(), "owner-1"));
        assertFalse(dao.deleteTroubleshootingForOwner(7, "owner-1"));

        assertTrue(jdbc.statements.isEmpty());
        assertEquals(3, jdbc.openCount);
        assertEquals(3, jdbc.closeCount);
    }

    @Test
    void detailLoadsStableOwnerIdForTheControllerAuthorizationDecision() {
        FakeJdbc jdbc = new FakeJdbc(true);
        jdbc.queryRow = Map.of(
                "id", 7,
                "creator_user_id", "owner-1",
                "creator", "Same Name");
        TroubleshootingDAO dao =
                new TroubleshootingDAO(
                        jdbc::open, new SchemaCapabilityCache());

        TroubleshootingDTO troubleshooting = dao.getTroubleshootingById(7);

        assertEquals("owner-1", troubleshooting.getCreatorUserId());
        assertEquals("Same Name", troubleshooting.getCreator());
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "updated_date, creator_user_id "
                        + "FROM troubleshooting WHERE id = ?"));
    }

    private static TroubleshootingDTO troubleshooting() {
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setId(7);
        troubleshooting.setTitle("Connection issue");
        troubleshooting.setCustomerName("Acme");
        troubleshooting.setCreatorUserId("owner-1");
        troubleshooting.setCreator("Same Name");
        return troubleshooting;
    }

    private static final class FakeJdbc {
        private final List<StatementRecord> statements = new ArrayList<>();
        private final boolean columnAvailable;
        private int openCount;
        private int closeCount;
        private Map<String, Object> queryRow;

        private FakeJdbc(boolean columnAvailable) {
            this.columnAvailable = columnAvailable;
        }

        private Connection open() {
            openCount++;
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "prepareStatement" -> statement((String) args[0]);
                        case "getMetaData" -> metadata();
                        case "close" -> {
                            closeCount++;
                            yield null;
                        }
                        case "isClosed" -> false;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private DatabaseMetaData metadata() {
            return (DatabaseMetaData) Proxy.newProxyInstance(
                    DatabaseMetaData.class.getClassLoader(),
                    new Class<?>[] {DatabaseMetaData.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getColumns" ->
                                columnResultSet(columnAvailable);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private PreparedStatement statement(String sql) {
            StatementRecord record = new StatementRecord(sql);
            statements.add(record);
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "setString", "setInt", "setTimestamp" -> {
                            record.parameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "setNull" -> {
                            record.parameters.put((Integer) args[0], null);
                            yield null;
                        }
                        case "executeUpdate" -> 1;
                        case "executeQuery" -> resultSet(queryRow);
                        case "close" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static ResultSet columnResultSet(boolean available) {
        boolean[] first = {available};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> {
                        boolean result = first[0];
                        first[0] = false;
                        yield result;
                    }
                    case "close" -> null;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static ResultSet resultSet(Map<String, Object> row) {
        boolean[] first = {row != null};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> {
                        boolean result = first[0];
                        first[0] = false;
                        yield result;
                    }
                    case "getString" -> {
                        Object value = row.get((String) args[0]);
                        yield value == null ? null : value.toString();
                    }
                    case "getInt" -> {
                        Object value = row.get((String) args[0]);
                        yield value instanceof Number number
                                ? number.intValue()
                                : 0;
                    }
                    case "getTimestamp", "close" -> null;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static final class StatementRecord {
        private final String sql;
        private final Map<Integer, Object> parameters =
                new LinkedHashMap<>();

        private StatementRecord(String sql) {
            this.sql = sql;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
