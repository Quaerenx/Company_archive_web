package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContinuousIntegrationContractTest {
    private static final Path WORKFLOW =
            Path.of(".github/workflows/ci.yml");
    private static final Path WRITE_E2E_WORKFLOW =
            Path.of(".github/workflows/isolated-write-e2e.yml");

    @Test
    void hostedCiRunsTheFullReadOnlyVerificationWithPinnedActions()
            throws Exception {
        String workflow = Files.readString(WORKFLOW);

        assertTrue(workflow.contains("pull_request:"));
        assertTrue(workflow.contains("branches:\n      - develop"));
        assertTrue(workflow.contains("permissions:\n  contents: read"));
        assertTrue(workflow.contains(
                "GRADLE_OPTS: --enable-native-access=ALL-UNNAMED"));
        assertTrue(workflow.contains("runs-on: ubuntu-24.04"));
        assertTrue(workflow.contains("timeout-minutes: 20"));
        assertTrue(workflow.contains(
                "run: ./gradlew --no-daemon clean check"));
        assertTrue(workflow.contains("java-version: \"25\""));
        assertTrue(workflow.contains("FROG2_JSPC_CATALINA_HOME:"));
        assertTrue(workflow.contains("sha512sum --check"));
        assertFalse(workflow.contains("runs-on: self-hosted"));
        assertFalse(workflow.contains("e2eWrite"));
        assertFalse(workflow.matches(
                "(?s).*uses:\\s+[^@\\s]+@(?:v|main|master)[^\\s]*.*"));
    }

    @Test
    void isolatedWriteCiIsManualDevelopOnlyAndUsesDedicatedInfrastructure()
            throws Exception {
        String workflow = Files.readString(WRITE_E2E_WORKFLOW);

        assertTrue(workflow.contains("workflow_dispatch:"));
        assertFalse(workflow.contains("pull_request:"));
        assertFalse(workflow.contains("push:"));
        assertTrue(workflow.contains(
                "if: github.ref == 'refs/heads/develop'"));
        assertTrue(workflow.contains(
                "runs-on: [self-hosted, linux, x64, frog2-isolated-e2e]"));
        assertTrue(workflow.contains("environment: frog2-isolated-e2e"));
        assertTrue(workflow.contains("FROG2_E2E_WRITE_ENABLED: \"true\""));
        assertTrue(workflow.contains(
                "FROG2_E2E_BASE_URL: http://127.0.0.1:19081/frog2/"));
        assertTrue(workflow.contains("./src/tools/isolated-write-e2e.sh"));
    }
}
