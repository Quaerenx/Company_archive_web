package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PasswordChangeViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void passwordFormAndVisualConstantsRemainStable() throws Exception {
        String page = read("mypage/change_password.jsp");
        assertTrue(page.contains("name=\"formAction\" value=\"updatePassword\""));
        assertTrue(page.contains("id=\"currentPassword\""));
        assertTrue(page.contains("id=\"newPassword\""));
        assertTrue(page.contains("id=\"confirmPassword\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("/resources/css/pages/password_change.css"));
        assertTrue(page.contains("account-form-container"));
        assertTrue(page.contains("/resources/js/pages/password_change.js"));
        assertFalse(page.contains("<style>"));
        assertFalse(page.contains("<script>"));
        assertFalse(page.contains("style=\""));
        assertFalse(page.contains("onsubmit="));

        String behavior = page.contains("/resources/js/pages/password_change.js")
                ? read("resources/js/pages/password_change.js")
                : page;
        assertTrue(behavior.contains("현재 비밀번호를 입력해주세요."));
        assertTrue(behavior.contains("새 비밀번호는 최소 8자 이상이어야 합니다."));
        assertTrue(behavior.contains("새 비밀번호가 일치하지 않습니다."));
        assertTrue(behavior.contains("현재 비밀번호와 새 비밀번호가 동일합니다."));
        assertTrue(behavior.contains("비밀번호를 변경하시겠습니까?"));

        String styles = read("resources/css/components.css")
                + "\n"
                + read("resources/css/pages/password_change.css");
        assertTrue(styles.contains("max-width: 600px"));
        assertTrue(styles.contains("background-color: #EEF1F6"));
        assertTrue(styles.contains("color: #3D5A80"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
