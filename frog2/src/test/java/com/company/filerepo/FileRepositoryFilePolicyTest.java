package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileRepositoryFilePolicyTest {
    private final FileRepositoryFilePolicy policy = new FileRepositoryFilePolicy();

    @Test
    void acceptsOnlyMatchingExtensionAndMimeType() throws Exception {
        var validated = policy.validate("보고서.pdf", "application/pdf; charset=binary", 128);

        assertEquals("보고서.pdf", validated.originalName());
        assertEquals("pdf", validated.extension());
        assertEquals("application/pdf", validated.contentType());
        assertEquals(5, FileRepositoryFilePolicy.MAX_FILE_COUNT);
        assertEquals(53_477_376L, FileRepositoryFilePolicy.MAX_REQUEST_SIZE);
    }

    @Test
    void rejectsActiveExtensionsAndMimeMismatch() {
        assertCode("unsupported_extension",
                () -> policy.validate("payload.jsp", "text/plain", 10));
        assertCode("unsupported_extension",
                () -> policy.validate("diagram.svg", "image/svg+xml", 10));
        assertCode("unsupported_media_type",
                () -> policy.validate("image.png", "text/html", 10));
        assertCode("invalid_filename",
                () -> policy.validate("../report.pdf", "application/pdf", 10));
    }

    @Test
    void rejectsExecutableAndEncodedHtmlContent() {
        assertCode("active_content", () -> policy.validateContent(new byte[] { 'M', 'Z', 0, 0 }));
        assertCode("active_content", () -> policy.validateContent("  <script>alert(1)</script>"
                .getBytes(StandardCharsets.UTF_8)));
        assertCode("active_content", () -> policy.validateContent(new byte[] {
                (byte) 0xff, (byte) 0xfe, '<', 0, 'h', 0, 't', 0, 'm', 0, 'l', 0
        }));
    }

    @Test
    void serverImportKeepsFilePolicyButNotMultipartSizeLimit()
            throws Exception {
        long largeSize = FileRepositoryFilePolicy.MAX_FILE_SIZE + 1;

        FileRepositoryException uploadError = assertThrows(
                FileRepositoryException.class,
                () -> policy.validate(
                        "archive.zip", "application/zip", largeSize));
        var imported = policy.validateImported("archive.zip", largeSize);

        assertEquals("file_too_large", uploadError.getCode());
        assertCode(
                "file_too_large",
                () -> policy.validateStored(
                        "archive.zip", "application/zip", largeSize));
        assertEquals("archive.zip", imported.originalName());
        assertEquals("application/zip", imported.contentType());
        assertCode(
                "unsupported_extension",
                () -> policy.validateImported("payload.sh", largeSize));
        assertCode(
                "unsupported_extension",
                () -> policy.validate(
                        "package.rpm", "application/x-rpm", 128));
        var rpm = policy.validateImported("package.rpm", largeSize);
        assertEquals("application/x-rpm", rpm.contentType());
    }

    private static void assertCode(String expectedCode, ThrowingCall call) {
        FileRepositoryException error = assertThrows(FileRepositoryException.class, call::run);
        assertEquals(expectedCode, error.getCode());
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
