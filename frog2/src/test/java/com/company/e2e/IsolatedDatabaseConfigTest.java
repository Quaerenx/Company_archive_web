package com.company.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IsolatedDatabaseConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsTheSharedConfigurationFileItself() throws Exception {
        Path shared = config("shared.properties", "jdbc:test:shared", false);

        assertThrows(
                IllegalArgumentException.class,
                () -> IsolatedDatabaseConfig.load(shared, shared));
    }

    @Test
    void rejectsAnIsolatedLabelThatStillTargetsTheSharedUrl()
            throws Exception {
        Path shared = config("shared.properties", "jdbc:test:shared", false);
        Path isolated = config(
                "isolated.properties", "jdbc:test:shared", true);

        assertThrows(
                IllegalArgumentException.class,
                () -> IsolatedDatabaseConfig.load(isolated, shared));
    }

    @Test
    void rejectsASeparateUrlWithoutTheExplicitIsolationMarker()
            throws Exception {
        Path shared = config("shared.properties", "jdbc:test:shared", false);
        Path isolated = config(
                "isolated.properties", "jdbc:test:isolated", false);

        assertThrows(
                IllegalArgumentException.class,
                () -> IsolatedDatabaseConfig.load(isolated, shared));
    }

    @Test
    void acceptsOnlyAnExplicitlyMarkedDifferentDatabase()
            throws Exception {
        Path shared = config(
                "shared.properties", "jdbc:test:shared", false, "shared-db");
        Path isolated = config(
                "isolated.properties", "jdbc:test:isolated", true, "isolated-db");

        Properties properties = IsolatedDatabaseConfig.load(
                isolated, shared);

        assertEquals("jdbc:test:isolated", properties.getProperty("db.url"));
    }

    @Test
    void rejectsDifferentUrlsThatDeclareTheSameDatabaseIdentity()
            throws Exception {
        Path shared = config(
                "shared.properties",
                "jdbc:test:database?role=reader",
                false,
                "archive-shared");
        Path isolated = config(
                "isolated.properties",
                "jdbc:test:database?role=writer",
                true,
                "archive-shared");

        assertThrows(
                IllegalArgumentException.class,
                () -> IsolatedDatabaseConfig.load(isolated, shared));
    }

    @Test
    void rejectsMissingDatabaseIdentityEvenWhenUrlsDiffer()
            throws Exception {
        Path shared = config(
                "shared.properties", "jdbc:test:shared", false, null);
        Path isolated = config(
                "isolated.properties", "jdbc:test:isolated", true, "isolated-db");

        assertThrows(
                IllegalArgumentException.class,
                () -> IsolatedDatabaseConfig.load(isolated, shared));
    }

    private Path config(String name, String url, boolean isolated)
            throws Exception {
        return config(name, url, isolated, null);
    }

    private Path config(
            String name,
            String url,
            boolean isolated,
            String databaseIdentity) throws Exception {
        Path path = temporaryDirectory.resolve(name);
        String contents = "db.url=" + url + "\n"
                + "db.user=test-user\n"
                + "db.password=test-password\n"
                + "db.driver=example.Driver\n"
                + "frog2.e2e.isolated=" + isolated + "\n"
                + (databaseIdentity == null
                        ? ""
                        : "frog2.databaseIdentity=" + databaseIdentity + "\n");
        Files.writeString(path, contents);
        return path;
    }
}
