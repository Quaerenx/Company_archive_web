package com.company.model;

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
    void updateUserNameDoesNotWriteDepartment() {
        AtomicReference<String> preparedSql = new AtomicReference<>();
        Map<Integer, String> bindings = new HashMap<>();
        UserDAO dao = new UserDAO(() -> updateConnection(preparedSql, bindings));

        assertTrue(dao.updateUserName("tester", "Updated User"));

        assertEquals(
                "UPDATE company_users SET userName = ? WHERE userId = ?",
                preparedSql.get());
        assertEquals(Map.of(1, "Updated User", 2, "tester"), bindings);
    }

    private static Connection connection(
            ResultSet resultSet, AtomicReference<String> boundUserId) {
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
