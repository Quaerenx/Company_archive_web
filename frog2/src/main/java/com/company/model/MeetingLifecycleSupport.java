package com.company.model;

import java.sql.Connection;
import java.sql.SQLException;

final class MeetingLifecycleSupport {
    static final String MIGRATION = "V20261006_17";

    private MeetingLifecycleSupport() {
    }

    static boolean enabled(Connection connection, SchemaCapabilityCache capabilities)
            throws SQLException {
        boolean deletedAt = capabilities.columnExists(connection, "meeting_records", "deleted_at");
        boolean deletedBy = capabilities.columnExists(connection, "meeting_records", "deleted_by");
        if (deletedAt != deletedBy) {
            throw new SQLException("Meeting soft-delete schema is partially applied");
        }
        return deletedAt;
    }

    static String active(Connection connection, SchemaCapabilityCache capabilities)
            throws SQLException {
        return enabled(connection, capabilities) ? "deleted_at IS NULL" : "1 = 1";
    }

    static String activeParent(Connection connection, SchemaCapabilityCache capabilities)
            throws SQLException {
        return "EXISTS (SELECT 1 FROM meeting_records WHERE meeting_records.meeting_id "
                + "= meeting_comments.meeting_id AND " + active(connection, capabilities) + ")";
    }
}
