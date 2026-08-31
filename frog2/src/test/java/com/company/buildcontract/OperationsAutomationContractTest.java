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

    @Test
    void writeE2eRejectsSharedTomcatAndStaleWar() throws Exception {
        String build = Files.readString(Path.of("build.gradle"));

        assertTrue(build.contains("verifyIsolatedE2eDeployment"));
        assertTrue(build.contains("[18081, 8080].contains(targetUri.port)"));
        assertTrue(build.contains("/opt/frog2-dev/e2e"));
        assertTrue(build.contains("currentHash != deployedHash"));
        assertTrue(build.contains(
                "tasks.named('e2eWrite') {\n    dependsOn verifyIsolatedE2eDeployment"));
    }

    @Test
    void customerAuditMigrationRemainsExplicitAndBackupGated()
            throws Exception {
        String build = Files.readString(Path.of("build.gradle"));
        String migrationTest = Files.readString(Path.of(
                "src/test/java/com/company/model/"
                        + "CustomerAuditMigrationE2ETest.java"));

        assertTrue(build.contains("'e2e-customer-audit-migration'"));
        assertTrue(build.contains("customerAuditMigration"));
        assertTrue(migrationTest.contains(
                "FROG2_CUSTOMER_AUDIT_MIGRATION_APPROVED"));
        assertTrue(migrationTest.contains("FROG2_CUSTOMER_AUDIT_BACKUP"));
        assertTrue(migrationTest.contains(
                "Schema migration must not change customer rows"));
    }
}
