package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryImporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void failedPublishRestoresOriginalWhileOtherFilesRemainImported()
            throws Exception {
        Path root = Files.createDirectory(
                temporaryDirectory.resolve("repository"));
        Path good = Files.writeString(root.resolve("good.txt"), "good");
        Path failed = Files.writeString(root.resolve("failed.txt"), "failed");
        markStable(good);
        markStable(failed);
        AtomicInteger invalidations = new AtomicInteger();
        FileRepositoryImporter importer = new FileRepositoryImporter(
                new FileRepositoryPathPolicy(root),
                new FileRepositoryFilePolicy(),
                (metadata, data, storageId) -> {
                    throw new AssertionError("No managed metadata is expected");
                },
                path -> invalidations.incrementAndGet(),
                (source, data) -> {
                    if (source.getFileName().toString().equals("failed.txt")) {
                        throw new IOException("simulated publish failure");
                    }
                });

        FileRepositoryImporter.Result result = importer.importUnmanaged("");

        assertEquals(1, result.importedCount());
        assertEquals(0, result.rejectedCount());
        assertEquals(0, result.deferredCount());
        assertEquals(1, result.failedCount());
        assertFalse(Files.exists(good));
        assertTrue(Files.exists(failed));
        assertEquals(1, invalidations.get());
        try (var children = Files.list(root)) {
            var names = children
                    .map(path -> path.getFileName().toString())
                    .toList();
            assertEquals(
                    1,
                    names.stream()
                            .filter(name -> name.endsWith(".data"))
                            .count());
            assertEquals(
                    1,
                    names.stream()
                            .filter(name -> name.endsWith(".meta"))
                            .count());
            assertFalse(names.stream().anyMatch(name -> name.endsWith(".tmp")));
        }
    }

    private static void markStable(Path path) throws IOException {
        Files.setLastModifiedTime(
                path,
                FileTime.from(Instant.now().minus(Duration.ofMinutes(1))));
    }
}
