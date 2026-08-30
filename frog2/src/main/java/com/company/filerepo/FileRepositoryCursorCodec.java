package com.company.filerepo;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class FileRepositoryCursorCodec {
    private static final int MAX_CURSOR_BYTES = 2048;

    private FileRepositoryCursorCodec() {
    }

    static String encode(SortKey key) {
        String value = key.kind() + "\u0000" + key.foldedName() + "\u0000"
                + key.name() + "\u0000" + key.uniqueId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    static SortKey decode(String rawCursor)
            throws FileRepositoryException {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(rawCursor.trim());
            if (decoded.length == 0 || decoded.length > MAX_CURSOR_BYTES) {
                throw new IllegalArgumentException();
            }
            String[] parts = new String(decoded, StandardCharsets.UTF_8)
                    .split("\u0000", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException();
            }
            int kind = Integer.parseInt(parts[0]);
            if ((kind != 0 && kind != 1)
                    || parts[1].isEmpty()
                    || parts[2].isEmpty()
                    || parts[3].isEmpty()) {
                throw new IllegalArgumentException();
            }
            return new SortKey(kind, parts[1], parts[2], parts[3]);
        } catch (IllegalArgumentException exception) {
            throw new FileRepositoryException(
                    400, "invalid_cursor", "Repository cursor is invalid");
        }
    }

    record SortKey(
            int kind, String foldedName, String name, String uniqueId)
            implements Comparable<SortKey> {
        @Override
        public int compareTo(SortKey other) {
            int comparison = Integer.compare(kind, other.kind);
            if (comparison == 0) {
                comparison = foldedName.compareTo(other.foldedName);
            }
            if (comparison == 0) {
                comparison = name.compareTo(other.name);
            }
            return comparison == 0
                    ? uniqueId.compareTo(other.uniqueId)
                    : comparison;
        }
    }
}
