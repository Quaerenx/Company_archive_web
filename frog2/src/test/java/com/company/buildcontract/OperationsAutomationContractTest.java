package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OperationsAutomationContractTest {
    @Test
    void deploymentRequiresVerifiedHashAndProductionApproval()
            throws Exception {
        String script = Files.readString(Path.of(
                "src/tools/deploy-war.sh"));

        assertTrue(script.contains("--sha256"));
        assertTrue(script.contains("frog2-release-manifest.txt"));
        assertTrue(script.contains("FROG2_PRODUCTION_DEPLOY_APPROVED"));
        assertTrue(script.contains(
                "FROG2_DEVELOPMENT_DIRTY_DEPLOY_APPROVED"));
        assertTrue(script.contains(
                "Production deployment refuses a dirty working-tree build"));
        assertTrue(script.contains("/health/ready"));
        assertTrue(script.contains("rollback"));
    }

    @Test
    void backupPruningIsDryRunAndApprovalGatedByDefault()
            throws Exception {
        String script = Files.readString(Path.of(
                "src/tools/prune-deploy-backups.sh"));

        assertTrue(script.contains("APPLY=false"));
        assertTrue(script.contains("FROG2_BACKUP_PRUNE_APPROVED"));
        assertTrue(script.contains("frog2-deploy-????????_??????"));
    }

    @Test
    void authenticatedSmokeRequiresExternalCredentials()
            throws Exception {
        String script = Files.readString(Path.of(
                "src/tools/authenticated-smoke.sh"));

        assertTrue(script.contains("FROG2_E2E_USER_ID"));
        assertTrue(script.contains("FROG2_E2E_PASSWORD"));
        assertTrue(script.contains("e2eAuthenticatedSmoke"));
    }
}
