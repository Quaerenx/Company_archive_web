package com.company.util;

import com.company.performance.JdbcConnectionAcquisition.ConnectionSupplier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class JdbcReadinessValidation {
    private JdbcReadinessValidation() {
    }

    static boolean check(ConnectionSupplier connections) {
        try (Connection connection = connections.get();
                Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(1);
            try (ResultSet result = statement.executeQuery("SELECT 1")) {
                return result.next() && result.getInt(1) == 1;
            }
        } catch (SQLException exception) {
            return false;
        }
    }
}
