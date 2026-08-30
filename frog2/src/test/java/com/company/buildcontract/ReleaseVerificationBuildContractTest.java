package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReleaseVerificationBuildContractTest {
    @Test
    void e2eTasksRequireTheBuiltAndSelectedServerWarToMatch()
            throws Exception {
        String build = Files.readString(Path.of("build.gradle"));

        assertTrue(build.contains(
                "tasks.register(\n        'verifyE2eDeployment')"));
        assertTrue(build.contains(
                "'/opt/tomcat-dev/webapps/frog2.war'"));
        assertTrue(build.contains(
                "'/opt/tomcat-prod-base/webapps/frog2.war'"));
        assertTrue(build.contains(
                "'E2E base URL must be the approved loopback application URL'"));
        assertTrue(build.contains(
                "!['127.0.0.1', 'localhost'].contains(targetHost)"));
        assertTrue(build.contains(
                "if (!configuredWarPath.isAbsolute())"));
        assertTrue(build.contains(
                "if (configuredWarPath != normalizedWarPath\n"
                        + "                || normalizedWarPath != canonicalWarPath)"));
        assertTrue(build.contains(
                "!canonicalWarPath.startsWith(canonicalBase)"));
        assertTrue(build.contains(
                "'E2E WAR must be the approved deployment artifact'"));
        assertTrue(build.contains(
                "'E2E WAR path must not contain symbolic links or aliases'"));
        assertFalse(build.contains(
                "'/opt/tomcat/webapps/frog2.war'"));
        assertTrue(build.contains(
                "tasks.named('e2eSmoke') {\n"
                        + "    dependsOn verifyE2eDeployment"));
        assertTrue(build.contains(
                "tasks.named('e2eAuthenticatedSmoke') {\n"
                        + "    dependsOn verifyE2eDeployment"));
    }
}
