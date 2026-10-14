package com.company.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CredentialFingerprint {
    private CredentialFingerprint() {
    }

    public static String from(String userId, String passwordHash) {
        if (userId == null || userId.isBlank()
                || passwordHash == null || passwordHash.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(userId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static boolean matches(String saved, String current) {
        return isValid(saved) && isValid(current)
                && MessageDigest.isEqual(
                        saved.getBytes(StandardCharsets.US_ASCII),
                        current.getBytes(StandardCharsets.US_ASCII));
    }

    public static boolean isValid(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
