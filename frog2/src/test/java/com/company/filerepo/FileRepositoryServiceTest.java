package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.performance.RequestPerformanceContext;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
    void importsStableServerFilesRecursivelyAndLeavesExcludedFilesUntouched()
            throws Exception {
        Path nested = Files.createDirectories(root.resolve("RPM").resolve("packages"));
        Path rootFile = Files.writeString(root.resolve("readme.txt"), "root file");
        Path nestedFile = Files.writeString(nested.resolve("package.log"), "nested file");
        Path hidden = Files.writeString(root.resolve(".secret.txt"), "hidden");
        Path active = Files.writeString(root.resolve("script.sh"), "#!/bin/sh");
        Path disguisedActive = Files.writeString(
                root.resolve("payload.txt"), "<html><script>alert(1)</script></html>");
        Path disguisedExecutable = Files.write(
                root.resolve("binary.txt"), new byte[] {'M', 'Z', 0, 0});
        Path outside = Files.writeString(
                temporaryDirectory.resolve("outside.txt"), "outside");
        Path link = root.resolve("linked.txt");
        Files.createSymbolicLink(link, outside);
        markStable(rootFile);
        markStable(nestedFile);
        markStable(hidden);
        markStable(active);
        markStable(disguisedActive);
        markStable(disguisedExecutable);

        FileRepositoryService.ImportResult result = service.importUnmanaged("");

        assertEquals(2, result.importedCount());
        assertEquals(0, result.conflictCount());
        assertEquals(5, result.rejectedCount());
        assertEquals(0, result.deferredCount());
        assertEquals(0, result.failedCount());
        assertFalse(Files.exists(rootFile));
        assertFalse(Files.exists(nestedFile));
        assertTrue(Files.exists(hidden));
        assertTrue(Files.exists(active));
        assertTrue(Files.exists(disguisedActive));
        assertTrue(Files.exists(disguisedExecutable));
        assertTrue(Files.isSymbolicLink(link));
        assertEquals(1, service.list("").getFileCount());
        assertEquals(1, service.list("RPM/packages").getFileCount());
    }

    @Test
    void importDefersRecentlyModifiedFilesAndReportsNameConflicts()
            throws Exception {
        byte[] existingContent = "managed".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload(
                "same.txt", "text/plain", existingContent.length);
        service.store(
                "",
                validated,
                existingContent.length,
                new ByteArrayInputStream(existingContent));
        Path conflict = Files.writeString(root.resolve("same.txt"), "copied");
        markStable(conflict);
        Path recent = Files.writeString(root.resolve("recent.txt"), "copying");

        FileRepositoryService.ImportResult result = service.importUnmanaged("");

        assertEquals(0, result.importedCount());
        assertEquals(1, result.conflictCount());
        assertEquals(0, result.rejectedCount());
        assertEquals(1, result.deferredCount());
        assertEquals(0, result.failedCount());
        assertTrue(Files.exists(conflict));
        assertTrue(Files.exists(recent));
    }

    @Test
    void importsServerFileLargerThanBrowserUploadLimit() throws Exception {
        Path large = root.resolve("package.rpm");
        long largeSize = FileRepositoryFilePolicy.MAX_FILE_SIZE + 1;
        try (var output = Files.newByteChannel(
                large,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            output.position(largeSize - 1);
            output.write(ByteBuffer.wrap(new byte[] {0}));
        }
        markStable(large);

        FileRepositoryService.ImportResult result = service.importUnmanaged("");
        FileRepositoryEntry entry = service.list("").getEntries().getFirst();
        FileRepositoryService.DownloadFile download =
                service.openDownload("", entry.getId());

        assertEquals(1, result.importedCount());
        assertEquals(largeSize, entry.getSize());
        assertEquals("RPM 패키지", entry.getDescription());
        assertEquals(largeSize, download.size());
        try (var children = Files.list(root)) {
            Path metadata = children
                    .filter(path -> path.getFileName().toString().endsWith(".meta"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(Files.readString(metadata).contains(
                    "source=server-import"));
        }
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
    void listingCountsOnlyInvalidManagedPairs() throws Exception {
        byte[] content = "safe".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload(
                "safe.txt", "text/plain", content.length);
        var stored = service.store(
                "",
                validated,
                content.length,
                new ByteArrayInputStream(content));

        assertEquals(0, service.list("").getInvalidEntryCount());

        Path metadataPath = managedPath(stored.id(), "meta");
        Files.writeString(
                metadataPath,
                Files.readString(metadataPath).replace("size=4", "size=9"));
        Files.writeString(managedPath("f".repeat(32), "data"), "orphan");
        Files.writeString(root.resolve("ordinary.txt"), "not indexed");
        Files.setLastModifiedTime(
                root,
                FileTime.fromMillis(System.currentTimeMillis() + 2_000));

        FileRepositoryListing listing = service.list("");

        assertEquals(2, listing.getInvalidEntryCount());
        assertEquals(0, listing.getFileCount());
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
        assertEquals(1, first.getCurrentPage());
        assertEquals(3, first.getTotalPages());
        assertEquals(5, first.getTotalCount());
        assertFalse(first.isHasPrevious());
        assertNull(first.getPreviousCursor());
        assertTrue(first.isHasNext());
        assertEquals(List.of("charlie", "delta"), names(second));
        assertEquals(2, second.getCurrentPage());
        assertEquals(3, second.getTotalPages());
        assertTrue(second.isHasPrevious());
        assertNull(second.getPreviousCursor());
        assertTrue(second.isHasNext());
        assertEquals(List.of("Echo"), names(third));
        assertEquals(3, third.getCurrentPage());
        assertEquals(3, third.getTotalPages());
        assertTrue(third.isHasPrevious());
        assertEquals(first.getNextCursor(), third.getPreviousCursor());
        assertFalse(third.isHasNext());
        assertEquals(1, service.snapshotScanCount());
        assertEquals(5, first.getDirectoryCount());
        assertEquals(5, second.getDirectoryCount());
        assertEquals(5, third.getDirectoryCount());
    }

    @Test
    void staleCursorFallsBackToTheLastNonEmptyPage() throws Exception {
        for (String name : List.of("Echo", "alpha", "delta", "Bravo", "charlie")) {
            Files.createDirectory(root.resolve(name));
        }

        FileRepositoryListing first = service.list("", null, 2);
        FileRepositoryListing second = service.list("", first.getNextCursor(), 2);
        String staleCursor = second.getNextCursor();

        Files.delete(root.resolve("Echo"));
        Files.setLastModifiedTime(
                root,
                FileTime.fromMillis(System.currentTimeMillis() + 2_000));

        FileRepositoryListing recovered = service.list("", staleCursor, 2);

        assertEquals(List.of("charlie", "delta"), names(recovered));
        assertEquals(2, recovered.getCurrentPage());
        assertEquals(2, recovered.getTotalPages());
        assertEquals(4, recovered.getTotalCount());
        assertTrue(recovered.isHasPrevious());
        assertFalse(recovered.isHasNext());
    }

    @Test
    void directorySnapshotRefreshesOnlyAfterTheDirectoryChanges()
            throws Exception {
        Files.createDirectory(root.resolve("alpha"));

        assertEquals(List.of("alpha"), names(service.list("")));
        assertEquals(List.of("alpha"), names(service.list("")));
        assertEquals(1, service.snapshotScanCount());

        Files.createDirectory(root.resolve("beta"));
        Files.setLastModifiedTime(
                root,
                FileTime.fromMillis(System.currentTimeMillis() + 2_000));

        assertEquals(List.of("alpha", "beta"), names(service.list("")));
        assertEquals(2, service.snapshotScanCount());
    }

    @Test
    void requestMetricsDistinguishColdScanFromWarmCacheHit()
            throws Exception {
        RequestPerformanceContext.begin();

        service.list("");
        service.list("");
        RequestPerformanceContext.Snapshot performance =
                RequestPerformanceContext.finish();

        assertEquals(1, performance.fileSnapshotCacheMisses());
        assertEquals(1, performance.fileSnapshotCacheHits());
        assertEquals(1, performance.fileSnapshotScanCount());
        assertTrue(performance.fileSnapshotScanDurationNanos() > 0);
    }

    @Test
    void uploadInvalidatesSnapshotsSharedBySeparateServletServices()
            throws Exception {
        assertEquals(0, service.list("").getFileCount());
        assertEquals(1, service.snapshotScanCount());

        FileRepositoryService uploadService =
                new FileRepositoryService(root);
        byte[] content = "shared cache invalidation"
                .getBytes(StandardCharsets.UTF_8);
        var validated = uploadService.validateUpload(
                "shared.txt", "text/plain", content.length);
        uploadService.store(
                "",
                validated,
                content.length,
                new ByteArrayInputStream(content));

        assertEquals(1, service.list("").getFileCount());
        assertEquals(2, service.snapshotScanCount());
    }

    @RepeatedTest(10)
    void concurrentColdRequestsShareOneDirectoryScan() throws Exception {
        Files.createDirectory(root.resolve("alpha"));
        CountDownLatch scanStarted = new CountDownLatch(1);
        CountDownLatch releaseScan = new CountDownLatch(1);
        service = new FileRepositoryService(root, path -> {
            scanStarted.countDown();
            awaitUnchecked(releaseScan);
        });
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch callersReady = new CountDownLatch(8);
        CountDownLatch releaseCallers = new CountDownLatch(1);
        List<Future<FileRepositoryListing>> listings = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                listings.add(executor.submit(() -> {
                    callersReady.countDown();
                    awaitUnchecked(releaseCallers);
                    return service.list("");
                }));
            }
            assertTrue(callersReady.await(5, TimeUnit.SECONDS));
            releaseCallers.countDown();
            assertTrue(scanStarted.await(5, TimeUnit.SECONDS));
            releaseScan.countDown();

            for (Future<FileRepositoryListing> listing : listings) {
                assertEquals(List.of("alpha"), names(listing.get(5, TimeUnit.SECONDS)));
            }
        } finally {
            releaseCallers.countDown();
            releaseScan.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, service.snapshotScanCount());
    }

    @Test
    void scanOfOneDirectoryDoesNotBlockAnotherDirectory() throws Exception {
        Files.createDirectory(root.resolve("alpha"));
        Files.createDirectory(root.resolve("beta"));
        CountDownLatch alphaStarted = new CountDownLatch(1);
        CountDownLatch releaseAlpha = new CountDownLatch(1);
        service = new FileRepositoryService(root, path -> {
            if (path.getFileName().toString().equals("alpha")) {
                alphaStarted.countDown();
                awaitUnchecked(releaseAlpha);
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<FileRepositoryListing> alpha =
                    executor.submit(() -> service.list("alpha"));
            assertTrue(alphaStarted.await(5, TimeUnit.SECONDS));

            Future<FileRepositoryListing> beta =
                    executor.submit(() -> service.list("beta"));
            assertTrue(beta.get(5, TimeUnit.SECONDS).getEntries().isEmpty());

            releaseAlpha.countDown();
            assertTrue(alpha.get(5, TimeUnit.SECONDS).getEntries().isEmpty());
        } finally {
            releaseAlpha.countDown();
            executor.shutdownNow();
        }

        assertEquals(2, service.snapshotScanCount());
    }

    @Test
    void failedColdScanDoesNotPoisonTheNextRequest() throws Exception {
        AtomicBoolean failFirstScan = new AtomicBoolean(true);
        service = new FileRepositoryService(root, path -> {
            if (failFirstScan.getAndSet(false)) {
                throw new IllegalStateException("simulated scan failure");
            }
        });

        assertThrows(IllegalStateException.class, () -> service.list(""));
        assertTrue(service.list("").getEntries().isEmpty());
        assertEquals(2, service.snapshotScanCount());
    }

    @Test
    void invalidationDuringColdScanPublishesFreshSnapshot() throws Exception {
        CountDownLatch scanStarted = new CountDownLatch(1);
        CountDownLatch releaseScan = new CountDownLatch(1);
        AtomicInteger scanAttempts = new AtomicInteger();
        service = new FileRepositoryService(root, path -> {
            if (scanAttempts.incrementAndGet() == 1) {
                scanStarted.countDown();
                awaitUnchecked(releaseScan);
            }
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<FileRepositoryListing> listing =
                    executor.submit(() -> service.list(""));
            assertTrue(scanStarted.await(5, TimeUnit.SECONDS));

            FileRepositoryService uploadService =
                    new FileRepositoryService(root);
            byte[] content = "fresh snapshot".getBytes(StandardCharsets.UTF_8);
            var validated = uploadService.validateUpload(
                    "fresh.txt", "text/plain", content.length);
            uploadService.store(
                    "", validated, content.length,
                    new ByteArrayInputStream(content));
            Files.setLastModifiedTime(
                    root,
                    FileTime.fromMillis(System.currentTimeMillis() + 2_000));
            releaseScan.countDown();

            assertEquals(1, listing.get(5, TimeUnit.SECONDS).getFileCount());
        } finally {
            releaseScan.countDown();
            executor.shutdownNow();
        }

        assertTrue(service.snapshotScanCount() >= 2);
        assertEquals(1, service.list("").getFileCount());
    }

    @Test
    void requestAfterUploadDoesNotJoinAnInvalidatedColdLoad()
            throws Exception {
        CountDownLatch firstScanStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstScan = new CountDownLatch(1);
        AtomicInteger scanAttempts = new AtomicInteger();
        service = new FileRepositoryService(root, path -> {
            if (scanAttempts.incrementAndGet() == 1) {
                firstScanStarted.countDown();
                awaitUnchecked(releaseFirstScan);
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<FileRepositoryListing> beforeUpload =
                    executor.submit(() -> service.list(""));
            assertTrue(firstScanStarted.await(5, TimeUnit.SECONDS));

            FileRepositoryService uploadService =
                    new FileRepositoryService(root);
            byte[] content = "published during cold scan"
                    .getBytes(StandardCharsets.UTF_8);
            var validated = uploadService.validateUpload(
                    "fresh.txt", "text/plain", content.length);
            uploadService.store(
                    "", validated, content.length,
                    new ByteArrayInputStream(content));

            Future<FileRepositoryListing> afterUpload =
                    executor.submit(() -> service.list(""));
            assertEquals(
                    1,
                    afterUpload.get(5, TimeUnit.SECONDS).getFileCount());

            releaseFirstScan.countDown();
            assertEquals(
                    1,
                    beforeUpload.get(5, TimeUnit.SECONDS).getFileCount());
        } finally {
            releaseFirstScan.countDown();
            executor.shutdownNow();
        }

        assertTrue(service.snapshotScanCount() >= 2);
    }

    @Test
    void repeatedInvalidationCannotStarveAColdListing() throws Exception {
        FileRepositoryService uploadService =
                new FileRepositoryService(root);
        AtomicInteger scanAttempts = new AtomicInteger();
        service = new FileRepositoryService(root, path -> {
            int attempt = scanAttempts.incrementAndGet();
            if (attempt > 12) {
                throw new IllegalStateException(
                        "snapshot retry limit was not enforced");
            }
            byte[] content = ("concurrent upload " + attempt)
                    .getBytes(StandardCharsets.UTF_8);
            try {
                var validated = uploadService.validateUpload(
                        "fresh-" + attempt + ".txt",
                        "text/plain",
                        content.length);
                uploadService.store(
                        "", validated, content.length,
                        new ByteArrayInputStream(content));
            } catch (FileRepositoryException exception) {
                throw new IllegalStateException(exception);
            }
        });

        FileRepositoryListing listing = service.list("");

        assertTrue(listing.getFileCount() > 0);
        assertTrue(scanAttempts.get() <= 6);
    }

    @Test
    void nextUploadQuarantinesStaleFilesFromAnInterruptedPublish()
            throws Exception {
        String orphanId = "1".repeat(32);
        Path orphanData = managedPath(orphanId, "data");
        Path uploadTemp = root.resolve(".frog2-upload-stale.tmp");
        Path metadataTemp = root.resolve(".frog2-meta-stale.tmp");
        Files.writeString(orphanData, "orphaned data");
        Files.writeString(uploadTemp, "partial upload");
        Files.writeString(metadataTemp, "partial metadata");
        FileTime stale = FileTime.from(
                Instant.now().minus(Duration.ofHours(2)));
        Files.setLastModifiedTime(orphanData, stale);
        Files.setLastModifiedTime(uploadTemp, stale);
        Files.setLastModifiedTime(metadataTemp, stale);

        byte[] content = "new safe upload".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload(
                "new.txt", "text/plain", content.length);
        service.store(
                "", validated, content.length,
                new ByteArrayInputStream(content));

        assertFalse(Files.exists(orphanData));
        assertFalse(Files.exists(uploadTemp));
        assertFalse(Files.exists(metadataTemp));
        Path quarantine = root.resolve(".frog2-quarantine");
        assertTrue(Files.isDirectory(quarantine));
        try (var files = Files.list(quarantine)) {
            assertEquals(3, files.count());
        }
        assertEquals(1, service.list("").getFileCount());
    }

    @Test
    void nextUploadDoesNotQuarantineAFreshUnpublishedDataFile()
            throws Exception {
        String activeId = "2".repeat(32);
        Path activeData = managedPath(activeId, "data");
        Files.writeString(activeData, "recent unpublished data");

        byte[] content = "new safe upload".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload(
                "new.txt", "text/plain", content.length);
        service.store(
                "", validated, content.length,
                new ByteArrayInputStream(content));

        assertTrue(Files.exists(activeData));
        assertFalse(Files.exists(root.resolve(".frog2-quarantine")));
        assertEquals(1, service.list("").getFileCount());
    }

    @Test
    void rejectsMalformedRepositoryCursor() {
        FileRepositoryException error = assertThrows(
                FileRepositoryException.class,
                () -> service.list("", "not-a-valid-cursor", 2));

        assertEquals(400, error.getHttpStatus());
        assertEquals("invalid_cursor", error.getCode());
    }

    @Test
    void laterSortingEntryDoesNotDuplicateTheCursorBoundary()
            throws Exception {
        for (String name : List.of("alpha", "bravo", "charlie", "delta")) {
            Files.createDirectory(root.resolve(name));
        }
        FileRepositoryListing first = service.list("", null, 2);

        Files.createDirectory(root.resolve("zulu"));
        Files.setLastModifiedTime(
                root,
                FileTime.fromMillis(System.currentTimeMillis() + 2_000));
        FileRepositoryListing second = service.list(
                "", first.getNextCursor(), 2);

        assertEquals(List.of("alpha", "bravo"), names(first));
        assertEquals(List.of("charlie", "delta"), names(second));
        assertFalse(names(second).stream().anyMatch(names(first)::contains));
        assertTrue(second.isHasNext());
    }

    private static List<String> names(FileRepositoryListing listing) {
        return listing.getEntries().stream()
                .map(FileRepositoryEntry::getName)
                .toList();
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("test coordination interrupted", exception);
        }
    }

    private Path managedPath(String id, String suffix) {
        return root.resolve(".frog2-" + id + "." + suffix);
    }

    private static void markStable(Path path) throws Exception {
        Files.setLastModifiedTime(
                path,
                FileTime.from(Instant.now().minus(Duration.ofMinutes(1))));
    }
}
