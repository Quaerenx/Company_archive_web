package com.company.util;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdbcConnectionFailureDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBConnection.class);

    private JdbcConnectionFailureDiagnostics() {
    }

    static void log(SQLException failure, DBConnection.PoolSnapshot pool) {
        String state = failure.getSQLState();
        String safeState = state != null && state.matches("[A-Z0-9]{5}")
                ? state : "unknown";
        LOGGER.warn(
                "JDBC connection acquisition failed sqlState={} errorCode={} "
                        + "poolAvailable={} active={} idle={} total={} waiting={}",
                safeState, failure.getErrorCode(), pool.available(), pool.active(),
                pool.idle(), pool.total(), pool.waiting());
    }
}
