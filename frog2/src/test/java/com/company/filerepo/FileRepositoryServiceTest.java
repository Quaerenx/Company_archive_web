package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryServiceTest {
    @TempDir
    Path temporaryDirectory;

    private Path root;
    private FileRepositoryService service;

    @BeforeEach
    void createRepository() throws Exception {
        root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        service = new FileRepositoryService(root);
    }

    @Test
    void storesWithServerNameAndDownloadsByOpaqueId() throws Exception {
        byte[] content = "safe repository content".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("고객 보고서.txt", "text/plain", content.length);

        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));
        var listing = service.list("");
        var download = service.openDownload("", stored.id());

        assertTrue(stored.id().matches("[0-9a-f]{32}"));
        assertEquals("고객 보고서.txt", listing.getEntries().getFirst().getName());
        assertEquals("고객 보고서.txt", download.originalName());
        assertArrayEquals(content, Files.readAllBytes(download.path()));
        try (var files = Files.list(root)) {
            List<String> serverNames = files.map(path -> path.getFileName().toString()).toList();
            assertEquals(2, serverNames.size());
            assertTrue(serverNames.stream().allMatch(name -> name.matches("\\.frog2-[0-9a-f]{32}\\.(data|meta)")));
            assertFalse(serverNames.contains("고객 보고서.txt"));
        }
    }

    @Test
    void ignoresUnmanagedFilesAndRollsBackOnlyNewManagedPair() throws Exception {
        Files.writeString(root.resolve("legacy-public.txt"), "do not expose");
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);
        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));

        assertEquals(1, service.list("").getFileCount());
        service.rollback(stored);

        assertEquals(0, service.list("").getFileCount());
        assertTrue(Files.exists(root.resolve("legacy-public.txt")));
    }

    @Test
    void activeContentFailureLeavesNoTemporaryOrManagedFiles() throws Exception {
        byte[] content = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("notes.txt", "text/plain", content.length);

        FileRepositoryException error = assertThrows(FileRepositoryException.class,
                () -> service.store("", validated, content.length, new ByteArrayInputStream(content)));

        assertEquals(415, error.getHttpStatus());
        try (var files = Files.list(root)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsDeclaredSizeMismatchAndPathTraversal() throws Exception {
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);

        FileRepositoryException mismatch = assertThrows(FileRepositoryException.class,
                () -> service.store("", validated, content.length + 1, new ByteArrayInputStream(content)));
        FileRepositoryException traversal = assertThrows(FileRepositoryException.class,
                () -> service.list("../outside"));

        assertEquals("size_mismatch", mismatch.getCode());
        assertEquals("invalid_path", traversal.getCode());
    }

    @Test
    void rejectsManagedFileWhenStoredSizeDoesNotMatchData() throws Exception {
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);
        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));
        Files.writeString(managedPath(stored.id(), "data"), "tampered-content");

        FileRepositoryException error = assertThrows(
                FileRepositoryException.class,
                () -> service.openDownload("", stored.id()));

        assertEquals("invalid_metadata", error.getCode());
        assertEquals(0, service.list("").getFileCount());
    }

    @Test
    void rejectsOversizedMetadataWithoutReadingItAsProperties() throws Exception {
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);
        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));
        Files.writeString(managedPath(stored.id(), "meta"), "x".repeat((8 * 1024) + 1));

        FileRepositoryException error = assertThrows(
                FileRepositoryException.class,
                () -> service.openDownload("", stored.id()));

        assertEquals("invalid_metadata", error.getCode());
        assertEquals(0, service.list("").getFileCount());
    }

    @Test
    void rejectsMissingOrNonNumericStoredSize() throws Exception {
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);
        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));
        Path metadataPath = managedPath(stored.id(), "meta");
        String metadata = Files.readString(metadataPath);
        Files.writeString(metadataPath, metadata.replace("size=4", "size=not-a-number"));

        FileRepositoryException error = assertThrows(
                FileRepositoryException.class,
                () -> service.openDownload("", stored.id()));

        assertEquals("invalid_metadata", error.getCode());
    }

    @Test
    void cursorPaginationKeepsStableFolderOrderAndBoundedPages()
            throws Exception {
        for (String name : List.of("Echo", "alpha", "delta", "Bravo", "charlie")) {
            Files.createDirectory(root.resolve(name));
        }

        FileRepositoryListing first = service.list("", null, 2);
        FileRepositoryListing second = service.list("", first.getNextCursor(), 2);
        FileRepositoryListing third = service.list("", second.getNextCursor(), 2);

        assertEquals(List.of("alpha", "Bravo"), names(first));
        assertTrue(first.isHasNext());
        assertEquals(List.of("charlie", "delta"), names(second));
        assertTrue(second.isHasNext());
        assertEquals(List.of("Echo"), names(third));
        assertFalse(third.isHasNext());
        assertEquals(5, first.getDirectoryCount());
        assertEquals(5, second.getDirectoryCount());
        assertEquals(5, third.getDirectoryCount());
    }

    @Test
    void rejectsMalformedRepositoryCursor() {
        FileRepositoryException error = assertThrows(
                FileRepositoryException.class,
                () -> service.list("", "not-a-valid-cursor", 2));

        assertEquals(400, error.getHttpStatus());
        assertEquals("invalid_cursor", error.getCode());
    }

    private static List<String> names(FileRepositoryListing listing) {
        return listing.getEntries().stream()
                .map(FileRepositoryEntry::getName)
                .toList();
    }

    private Path managedPath(String id, String suffix) {
        return root.resolve(".frog2-" + id + "." + suffix);
    }
}
