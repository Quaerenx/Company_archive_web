package com.company.model;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
interface JdbcConnectionProvider {
    Connection getConnection() throws SQLException;
}
