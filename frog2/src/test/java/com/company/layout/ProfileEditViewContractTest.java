package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfileEditViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void profileFormAndVisualConstantsRemainStable() throws Exception {
        String page = read("mypage/edit_profile.jsp");
        assertTrue(page.contains("name=\"formAction\" value=\"updateProfile\""));
        assertTrue(page.contains("id=\"userId\""));
        assertTrue(page.contains("id=\"userName\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("/resources/css/pages/profile_edit.css"));
        assertTrue(page.contains("/resources/js/pages/profile_edit.js"));
        assertTrue(page.contains("account-form-container"));
        assertFalse(page.contains("<style>"));
        assertFalse(page.contains("<script>"));
        assertFalse(page.contains("onsubmit="));

        String behavior = page.contains("/resources/js/pages/profile_edit.js")
                ? read("resources/js/pages/profile_edit.js")
                : page;
        assertTrue(behavior.contains("이름을 입력해주세요."));
        assertTrue(behavior.contains("프로필을 수정하시겠습니까?"));

        String styles = read("resources/css/components.css")
                + "\n"
                + read("resources/css/pages/profile_edit.css");
        assertTrue(styles.contains("max-width: 600px"));
        assertTrue(styles.contains("border-color: var(--color-focus)"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
