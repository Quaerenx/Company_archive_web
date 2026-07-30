package com.company.security;

import java.util.Optional;

public final class PasswordPolicy {
    public static final int MINIMUM_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static Optional<String> validate(String currentPassword, String newPassword, String confirmation) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return Optional.of("현재 비밀번호를 입력해주세요.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Optional.of("새 비밀번호를 입력해주세요.");
        }
        if (confirmation == null || !newPassword.equals(confirmation)) {
            return Optional.of("새 비밀번호가 일치하지 않습니다.");
        }
        if (newPassword.length() < MINIMUM_LENGTH) {
            return Optional.of("새 비밀번호는 최소 8자 이상이어야 합니다.");
        }
        if (currentPassword.equals(newPassword)) {
            return Optional.of("현재 비밀번호와 다른 비밀번호를 사용해주세요.");
        }
        return Optional.empty();
    }
}
