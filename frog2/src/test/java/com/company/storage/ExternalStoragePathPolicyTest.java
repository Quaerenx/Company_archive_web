package com.company.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalStoragePathPolicyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesOnlyAbsoluteExternalRoots() {
        Path configured = temporaryDirectory.resolve("data/../archive");

        assertEquals(
                temporaryDirectory.resolve("archive"),
                ExternalStoragePathPolicy.resolveRoot(
                        "prod",
                        configured.toString(),
                        "frog2.storageRoot",
                        "/unused",
                        null));
        assertThrows(
                IllegalStateException.class,
                () -> ExternalStoragePathPolicy.resolveRoot(
                        "prod",
                        "relative/archive",
                        "frog2.storageRoot",
                        "/unused",
                        null));
    }

    @Test
    void rejectsDeploymentPathsReachedThroughASymbolicLinkAlias()
            throws Exception {
        Path catalinaBase = Files.createDirectory(
                temporaryDirectory.resolve("tomcat"));
        Path webapps = Files.createDirectory(catalinaBase.resolve("webapps"));
        Path alias = temporaryDirectory.resolve("webapps-alias");
        Files.createSymbolicLink(alias, webapps);

        assertThrows(
                IllegalStateException.class,
                () -> ExternalStoragePathPolicy.resolveRoot(
                        "prod",
                        alias.resolve("frog2-data").toString(),
                        "frog2.storageRoot",
                        "/unused",
                        catalinaBase.toString()));
    }

    @Test
    void identifiesOnlyRealNonSymbolicDirectoriesAsSafe()
            throws Exception {
        Path directory = Files.createDirectory(
                temporaryDirectory.resolve("directory"));
        Path file = Files.writeString(
                temporaryDirectory.resolve("file"), "data");
        Path link = temporaryDirectory.resolve("directory-link");
        Files.createSymbolicLink(link, directory);

        assertTrue(ExternalStoragePathPolicy.isSafeDirectory(directory));
        assertFalse(ExternalStoragePathPolicy.isSafeDirectory(file));
        assertFalse(ExternalStoragePathPolicy.isSafeDirectory(link));
    }
}
