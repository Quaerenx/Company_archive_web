package com.company.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MigrationLedgerVerifier {
    private static final Set<String> ACCEPTED_DECISIONS =
            Set.of("applied", "baselined");

    private MigrationLedgerVerifier() {
    }

    static Result reconcile(
            List<MigrationManifest.Entry> migrations,
            List<MigrationLedgerRepository.Record> records) {
        Map<String, MigrationManifest.Entry> byVersion = new HashMap<>();
        for (MigrationManifest.Entry migration : migrations) {
            byVersion.put(migration.version(), migration);
        }

        Map<String, MigrationLedgerRepository.Record> recorded = new HashMap<>();
        for (MigrationLedgerRepository.Record record : records) {
            MigrationManifest.Entry migration = byVersion.get(record.version());
            if (migration == null) {
                throw new IllegalStateException(
                        "Ledger contains an unknown migration: " + record.version());
            }
            if (recorded.put(record.version(), record) != null) {
                throw new IllegalStateException(
                        "Ledger contains a duplicate migration: " + record.version());
            }
            if (!migration.filename().equals(record.filename())
                    || !migration.checksum().equals(record.checksum())) {
                throw new IllegalStateException(
                        "Ledger evidence does not match the manifest: " + record.version());
            }
            if (!ACCEPTED_DECISIONS.contains(record.decision())) {
                throw new IllegalStateException(
                        "Ledger decision is not deployable: " + record.version());
            }
        }

        List<String> pending = new ArrayList<>();
        for (MigrationManifest.Entry migration : migrations) {
            if (!recorded.containsKey(migration.version())) {
                pending.add(migration.version());
            }
        }
        return new Result(records.size(), List.copyOf(pending));
    }

    record Result(int recordedCount, List<String> pendingVersions) {
        boolean complete() {
            return pendingVersions.isEmpty();
        }
    }
}
