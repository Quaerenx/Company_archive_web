package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class BrandLogoAssetContractTest {
    private static final Path IMAGES = Path.of("src/main/webapp/resources/images");

    @Test
    void primaryLoginLogoMatchesTheApprovedTransparentAsset() throws Exception {
        assertLogo(
                "archive-primary-logo.svg",
                "1119",
                "288",
                "c9e6619a183007a456450b325b5f060d2ce7f2a4c10f7e48db282028b8cb9e14");
    }

    @Test
    void compactHeaderLogoMatchesTheApprovedTransparentAsset() throws Exception {
        assertLogo(
                "archive-compact-horizontal.svg",
                "373",
                "112",
                "0fba7ff20fbab28711fc5c62c7f5a5aa897b3fc1ac9840c0bf8e6a86172df8f5");
    }

    private static void assertLogo(
            String fileName,
            String width,
            String height,
            String expectedSha256) throws Exception {
        byte[] bytes = Files.readAllBytes(IMAGES.resolve(fileName));
        String svg = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String lower = svg.toLowerCase();

        assertEquals(
                expectedSha256,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertTrue(svg.contains("width=\"" + width + "\" height=\"" + height + "\""));
        assertTrue(svg.contains("viewBox=\"0 0 " + width + " " + height + "\""));
        assertEquals(1, occurrences(svg, "<image "));
        assertEquals(1, occurrences(svg, "href=\"data:image/png;base64,"));
        assertFalse(lower.contains("<script"));
        assertFalse(lower.contains("<foreignobject"));
        assertFalse(lower.contains("javascript:"));
        assertFalse(lower.contains("xlink:href"));
        assertFalse(lower.contains("onload="));
        assertFalse(lower.contains("onclick="));
        assertFalse(lower.contains("@import"));
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
