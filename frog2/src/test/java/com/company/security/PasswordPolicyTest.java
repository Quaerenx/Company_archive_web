package com.company.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void acceptsMatchingNonReusedPasswordAtMinimumLength() {
        assertTrue(PasswordPolicy.validate("old-password", "new-pass", "new-pass").isEmpty());
    }

    @Test
    void rejectsMissingMismatchedShortAndReusedPasswords() {
        assertEquals("현재 비밀번호를 입력해주세요.",
                PasswordPolicy.validate(null, "new-pass", "new-pass").orElseThrow());
        assertEquals("새 비밀번호를 입력해주세요.",
                PasswordPolicy.validate("old-pass", null, null).orElseThrow());
        assertEquals("새 비밀번호가 일치하지 않습니다.",
                PasswordPolicy.validate("old-pass", "new-pass", "different").orElseThrow());
        assertEquals("새 비밀번호는 최소 8자 이상이어야 합니다.",
                PasswordPolicy.validate("old-pass", "short", "short").orElseThrow());
        assertEquals("현재 비밀번호와 다른 비밀번호를 사용해주세요.",
                PasswordPolicy.validate("same-pass", "same-pass", "same-pass").orElseThrow());
    }
}
