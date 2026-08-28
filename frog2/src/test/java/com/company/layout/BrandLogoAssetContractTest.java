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
    void sharedArchiveLogoMatchesTheApprovedVectorAsset() throws Exception {
        byte[] bytes = Files.readAllBytes(IMAGES.resolve("archive-logo.svg"));
        String svg = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String lower = svg.toLowerCase();

        assertEquals(
                "0782b6e2859ead3652bf8c6d09e3f35d04a7263cae5de9a1771fc509b9049e6b",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertTrue(svg.contains("width=\"4096\" height=\"2286\""));
        assertTrue(svg.contains("viewBox=\"0 0 4096 2286\""));
        assertTrue(occurrences(svg, "<path ") >= 1);
        assertEquals(0, occurrences(svg, "<image "));
        assertFalse(lower.contains("href=\"data:"));
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
