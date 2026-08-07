package com.company.security;

import com.company.model.UserDTO;
import java.util.Arrays;

public final class AdminAccessPolicy {
    public static final String ADMIN_USER_IDS_PROPERTY = "frog2.adminUserIds";
    public static final String ADMIN_USER_IDS_ENVIRONMENT =
            "FROG2_ADMIN_USER_IDS";

    private AdminAccessPolicy() {
    }

    public static boolean isAdmin(UserDTO user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }
        String configured = System.getProperty(ADMIN_USER_IDS_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ADMIN_USER_IDS_ENVIRONMENT);
        }
        return containsUserId(configured, user.getUserId());
    }

    static boolean containsUserId(String configured, String userId) {
        if (configured == null
                || configured.isBlank()
                || userId == null
                || userId.isBlank()) {
            return false;
        }
        String expected = userId.trim();
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(expected::equals);
    }
}
