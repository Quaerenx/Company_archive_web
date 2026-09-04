package com.company.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationLedgerVerifierTest {
    private static final MigrationManifest.Entry MIGRATION =
            new MigrationManifest.Entry(
                    "V20260904_99",
                    "V20260904_99__test.sql",
                    "a".repeat(64),
                    Path.of("test.sql"));

    @Test
    void reportsUnrecordedVersionsAsPending() {
        MigrationLedgerVerifier.Result result =
                MigrationLedgerVerifier.reconcile(List.of(MIGRATION), List.of());

        assertEquals(List.of("V20260904_99"), result.pendingVersions());
        assertTrue(!result.complete());
    }

    @Test
    void acceptsMatchingAppliedEvidence() {
        MigrationLedgerVerifier.Result result = MigrationLedgerVerifier.reconcile(
                List.of(MIGRATION),
                List.of(new MigrationLedgerRepository.Record(
                        MIGRATION.version(),
                        MIGRATION.filename(),
                        MIGRATION.checksum(),
                        "applied")));

        assertTrue(result.complete());
        assertEquals(1, result.recordedCount());
    }

    @Test
    void rejectsChecksumDrift() {
        assertThrows(
                IllegalStateException.class,
                () -> MigrationLedgerVerifier.reconcile(
                        List.of(MIGRATION),
                        List.of(new MigrationLedgerRepository.Record(
                                MIGRATION.version(),
                                MIGRATION.filename(),
                                "b".repeat(64),
                                "applied"))));
    }
}
