package com.company.model;

import java.sql.Connection;

final class CustomerAuditSupport {
    static final String TABLE = "vertica_customer_detail";
    static final String UPDATED_AT = "updated_at";
    static final String UPDATED_BY = "updated_by";
    static final String DELETED_AT = "deleted_at";
    static final String DELETED_BY = "deleted_by";

    private CustomerAuditSupport() {
    }

    enum Capability {
        NONE,
        COMPLETE,
        PARTIAL
    }

    static Capability capability(
            Connection connection, SchemaCapabilityCache capabilities) {
        boolean updatedAtAvailable = capabilities.columnExists(
                connection, TABLE, UPDATED_AT);
        boolean updatedByAvailable = capabilities.columnExists(
                connection, TABLE, UPDATED_BY);
        boolean deletedAtAvailable = capabilities.columnExists(
                connection, TABLE, DELETED_AT);
        boolean deletedByAvailable = capabilities.columnExists(
                connection, TABLE, DELETED_BY);

        // updated_at predates the audit migration in legacy installations.
        if (!updatedByAvailable && !deletedAtAvailable && !deletedByAvailable) {
            return Capability.NONE;
        }
        if (updatedAtAvailable && updatedByAvailable
                && deletedAtAvailable && deletedByAvailable) {
            return Capability.COMPLETE;
        }
        return Capability.PARTIAL;
    }

    static boolean isAvailable(
            Connection connection, SchemaCapabilityCache capabilities) {
        Capability capability = capability(connection, capabilities);
        requireNotPartial(capability);
        return capability == Capability.COMPLETE;
    }

    static boolean shouldAuditWrite(
            Connection connection,
            SchemaCapabilityCache capabilities,
            String actorUserId) {
        Capability capability = capability(connection, capabilities);
        requireNotPartial(capability);
        if (capability == Capability.NONE) {
            return false;
        }
        if (!hasActor(actorUserId)) {
            throw new IllegalStateException(
                    "Customer audit actor is required when the audit schema is available");
        }
        return true;
    }

    private static void requireNotPartial(Capability capability) {
        if (capability == Capability.PARTIAL) {
            throw new IllegalStateException(
                    "Customer audit schema is partially applied");
        }
    }

    private static boolean hasActor(String actorUserId) {
        return actorUserId != null && !actorUserId.isBlank();
    }
}
