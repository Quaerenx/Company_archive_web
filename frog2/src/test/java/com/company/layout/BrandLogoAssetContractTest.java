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
                "cc65259e669c6a5b8e992c2fad3bb811be2b612ce915707ce66eb567303a190f",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertTrue(svg.contains("width=\"3664\" height=\"1480\""));
        // viewBox는 그림 경계에 맞춰져 있다. 캔버스에 여백이 남아 있으면
        // 로그인 카드와 헤더의 로고 종횡비가 어긋나 전환 모프가 출렁인다.
        assertTrue(svg.contains("viewBox=\"214 387 3664 1480\""));
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
