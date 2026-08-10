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
        Path shared = config("shared.properties", "jdbc:test:shared", false);
        Path isolated = config(
                "isolated.properties", "jdbc:test:isolated", true);

        Properties properties = IsolatedDatabaseConfig.load(
                isolated, shared);

        assertEquals("jdbc:test:isolated", properties.getProperty("db.url"));
    }

    private Path config(String name, String url, boolean isolated)
            throws Exception {
        Path path = temporaryDirectory.resolve(name);
        String contents = "db.url=" + url + "\n"
                + "db.user=test-user\n"
                + "db.password=test-password\n"
                + "db.driver=example.Driver\n"
                + "frog2.e2e.isolated=" + isolated + "\n";
        Files.writeString(path, contents);
        return path;
    }
}
