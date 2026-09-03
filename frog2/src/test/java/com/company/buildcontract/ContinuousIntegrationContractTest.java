package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContinuousIntegrationContractTest {
    private static final Path WORKFLOW =
            Path.of(".github/workflows/ci.yml");

    @Test
    void hostedCiRunsTheFullReadOnlyVerificationWithPinnedActions()
            throws Exception {
        String workflow = Files.readString(WORKFLOW);

        assertTrue(workflow.contains("pull_request:"));
        assertTrue(workflow.contains("branches:\n      - develop"));
        assertTrue(workflow.contains("permissions:\n  contents: read"));
        assertTrue(workflow.contains("runs-on: ubuntu-24.04"));
        assertTrue(workflow.contains("timeout-minutes: 20"));
        assertTrue(workflow.contains(
                "run: ./gradlew --no-daemon clean check"));
        assertTrue(workflow.contains("FROG2_JSPC_CATALINA_HOME:"));
        assertTrue(workflow.contains("sha512sum --check"));
        assertFalse(workflow.contains("runs-on: self-hosted"));
        assertFalse(workflow.contains("e2eWrite"));
        assertFalse(workflow.matches(
                "(?s).*uses:\\s+[^@\\s]+@(?:v|main|master)[^\\s]*.*"));
    }
}
