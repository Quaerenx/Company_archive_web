package com.company.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class JdbcConnectionFailureDiagnosticsTest {
    @Test
    void recordsSafeFailureFieldsAndPoolCountsWithoutExceptionDetails() {
        SQLException failure = new SQLException("password=secret-host", "08001", 123);
        ILoggingEvent event = logged(failure);

        String message = event.getFormattedMessage();
        assertTrue(message.contains("sqlState=08001 errorCode=123"));
        assertTrue(message.contains("active=20 idle=0 total=20 waiting=3"));
        assertFalse(message.contains("password"));
        assertFalse(message.contains("secret-host"));
        assertNull(event.getThrowableProxy());
    }

    @Test
    void malformedSqlStateCannotInjectSensitiveTextIntoLogs() {
        ILoggingEvent event = logged(new SQLException("secret", "password=secret\n", -1));

        assertTrue(event.getFormattedMessage().contains("sqlState=unknown"));
        assertFalse(event.getFormattedMessage().contains("secret"));
    }

    private static ILoggingEvent logged(SQLException failure) {
        Logger logger = (Logger) LoggerFactory.getLogger(DBConnection.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            JdbcConnectionFailureDiagnostics.log(
                    failure, new DBConnection.PoolSnapshot(true, 20, 0, 20, 3));
            return appender.list.getFirst();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
