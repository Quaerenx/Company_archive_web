package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryPathPolicyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesOnlyExistingContainedDirectories() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        Path child = Files.createDirectory(root.resolve("고객 자료"));
        FileRepositoryPathPolicy policy = new FileRepositoryPathPolicy(root);

        var resolved = policy.resolveExistingDirectory("고객 자료");

        assertEquals("고객 자료", resolved.relativePath());
        assertEquals(child.toRealPath(), resolved.path());
        assertStatus(400, () -> policy.resolveExistingDirectory("../outside"));
        assertStatus(400, () -> policy.resolveExistingDirectory("hidden/.secret"));
        assertStatus(404, () -> policy.resolveExistingDirectory("missing"));
    }

    @Test
    void rejectsSymlinkEscapeAndSymlinkRoot() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Path escape = root.resolve("escape");
        Files.createSymbolicLink(escape, outside);

        FileRepositoryPathPolicy policy = new FileRepositoryPathPolicy(root);

        assertStatus(404, () -> policy.resolveExistingDirectory("escape"));
        Path rootLink = temporaryDirectory.resolve("repository-link");
        Files.createSymbolicLink(rootLink, root);
        assertThrows(IOException.class, () -> new FileRepositoryPathPolicy(rootLink));
    }

    @Test
    void validatesManagedStorageIdentifiers() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        FileRepositoryPathPolicy policy = new FileRepositoryPathPolicy(root);
        var directory = policy.resolveExistingDirectory("");

        assertStatus(400, () -> policy.managedPathForWrite(directory, "../payload", ".data"));
        assertEquals(root.resolve(".frog2-0123456789abcdef0123456789abcdef.data"),
                policy.managedPathForWrite(directory, "0123456789abcdef0123456789abcdef", ".data"));
    }

    private static void assertStatus(int expectedStatus, ThrowingCall call) {
        FileRepositoryException error = assertThrows(FileRepositoryException.class, call::run);
        assertEquals(expectedStatus, error.getHttpStatus());
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
