package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.util.PasswordUtils;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class UserDAOContractTest {
    @Test
    void authenticateReturnsUserForMatchingCredentials() {
        AtomicReference<String> boundUserId = new AtomicReference<>();
        ResultSet resultSet = resultSet(Map.of(
                "userId", "tester",
                "password", PasswordUtils.hashPassword("correct-password"),
                "userName", "Test User"), true);
        UserDAO dao = new UserDAO(() -> connection(resultSet, boundUserId));

        UserDTO user = dao.authenticateUser("tester", "correct-password");

        assertNotNull(user);
        assertEquals("tester", user.getUserId());
        assertEquals("Test User", user.getUserName());
        assertEquals("tester", boundUserId.get());
    }

    @Test
    void authenticateReturnsNullWhenUserDoesNotExist() {
        UserDAO dao = new UserDAO(() -> connection(resultSet(Map.of(), false), new AtomicReference<>()));

        assertNull(dao.authenticateUser("missing", "password"));
    }

    @Test
    void authenticateThrowsExplicitExceptionForSqlFailure() {
        UserDAO dao = new UserDAO(() -> {
            throw new SQLException("database unavailable", "08001");
        });

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> dao.authenticateUser("tester", "password"));

        assertEquals(DataAccessException.Kind.GENERAL, exception.getKind());
    }

    @Test
    void passwordVerificationRunsAfterTheJdbcConnectionIsClosed() {
        AtomicBoolean connectionClosed = new AtomicBoolean();
        ResultSet resultSet = resultSet(Map.of(
                "userId", "tester",
                "password", "stored-hash",
                "userName", "Test User"), true);
        UserDAO dao = new UserDAO(
                () -> connection(
                        resultSet,
                        new AtomicReference<>(),
                        connectionClosed),
                new SchemaCapabilityCache(),
                (plainText, passwordHash) -> {
                    assertTrue(connectionClosed.get());
                    assertEquals("correct-password", plainText);
                    assertEquals("stored-hash", passwordHash);
                    return true;
                });

        UserDTO user = dao.authenticateUser(
                "tester", "correct-password");

        assertNotNull(user);
        assertTrue(connectionClosed.get());
    }

    @Test
    void updateUserNameDoesNotWriteDepartment() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        UserDAO dao = new UserDAO(jdbc::open);

        assertTrue(dao.updateUserName("tester", "Updated User"));

        assertEquals(
                "UPDATE company_users SET userName = ? WHERE userId = ?",
                jdbc.statements.getFirst().sql);
        assertEquals(
                Map.of(1, "Updated User", 2, "tester"),
                jdbc.statements.getFirst().parameters);
    }

    @Test
    void stableAssignmentsSurviveAUserDisplayNameChange() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = java.util.Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id");
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(2);
        jdbc.enqueueUpdate(1);
        UserDAO dao = new UserDAO(jdbc::open);

        assertTrue(dao.updateUserName("tester", "Renamed User"));

        assertEquals(3, jdbc.statements.size());
        assertEquals(
                "UPDATE company_users SET userName = ? WHERE userId = ?",
                jdbc.statements.get(0).sql);
        assertEquals(
                "UPDATE vertica_customer_detail SET main_manager = ? "
                        + "WHERE main_manager_user_id = ?",
                jdbc.statements.get(1).sql);
        assertEquals(
                "UPDATE vertica_customer_detail SET sub_manager = ? "
                        + "WHERE sub_manager_user_id = ?",
                jdbc.statements.get(2).sql);
        assertEquals(
                Map.of(1, "Renamed User", 2, "tester"),
                jdbc.statements.get(1).parameters);
        assertEquals(1, jdbc.commitCount);
        assertEquals(0, jdbc.rollbackCount);
        assertEquals(java.util.List.of(false, true), jdbc.autoCommitValues);
    }

    @Test
    void displayNameChangeRollsBackWhenAssignmentSyncFails() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = java.util.Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id");
        jdbc.enqueueUpdate(1);
        UserDAO dao = new UserDAO(jdbc::open);

        assertThrows(
                DataAccessException.class,
                () -> dao.updateUserName("tester", "Renamed User"));

        assertEquals(0, jdbc.commitCount);
        assertEquals(1, jdbc.rollbackCount);
        assertEquals(java.util.List.of(false, true), jdbc.autoCommitValues);
    }

    @Test
    void passwordHashingRunsBeforeTheJdbcConnectionIsAcquired() {
        AtomicBoolean connectionAcquired = new AtomicBoolean();
        AtomicReference<String> preparedSql = new AtomicReference<>();
        Map<Integer, String> bindings = new HashMap<>();
        UserDAO dao = new UserDAO(
                () -> {
                    connectionAcquired.set(true);
                    return updateConnection(preparedSql, bindings);
                },
                new SchemaCapabilityCache(),
                (plainText, passwordHash) -> false,
                plainText -> {
                    assertTrue(!connectionAcquired.get());
                    assertEquals("new-password", plainText);
                    return "new-hash";
                });

        assertTrue(dao.updatePassword("tester", "new-password"));

        assertTrue(connectionAcquired.get());
        assertEquals(Map.of(1, "new-hash", 2, "tester"), bindings);
    }

    private static Connection connection(
            ResultSet resultSet, AtomicReference<String> boundUserId) {
        return connection(
                resultSet, boundUserId, new AtomicBoolean());
    }

    private static Connection connection(
            ResultSet resultSet,
            AtomicReference<String> boundUserId,
            AtomicBoolean connectionClosed) {
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                (ignored, call, args) -> switch (call.getName()) {
                    case "setString" -> {
                        if ((Integer) args[0] == 1) {
                            boundUserId.set((String) args[1]);
                        }
                        yield null;
                    }
                    case "executeQuery" -> resultSet;
                    default -> defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (ignored, call, args) -> switch (call.getName()) {
                    case "prepareStatement" -> statement;
                    case "close" -> {
                        connectionClosed.set(true);
                        yield null;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static ResultSet resultSet(Map<String, String> values, boolean hasRow) {
        AtomicBoolean first = new AtomicBoolean(hasRow);
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] { ResultSet.class },
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> first.getAndSet(false);
                    case "getString" -> values.get((String) args[0]);
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static Connection updateConnection(
            AtomicReference<String> preparedSql, Map<Integer, String> bindings) {
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                (ignored, call, args) -> switch (call.getName()) {
                    case "setString" -> {
                        bindings.put((Integer) args[0], (String) args[1]);
                        yield null;
                    }
                    case "executeUpdate" -> 1;
                    default -> defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (ignored, call, args) -> switch (call.getName()) {
                    case "prepareStatement" -> {
                        preparedSql.set((String) args[0]);
                        yield statement;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

}
