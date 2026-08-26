package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReleaseVerificationBuildContractTest {
    @Test
    void e2eTasksRequireTheBuiltAndDevelopmentWarToMatch()
            throws Exception {
        String build = Files.readString(Path.of("build.gradle"));

        assertTrue(build.contains(
                "tasks.register(\n        'verifyDevelopmentDeployment')"));
        assertTrue(build.contains(
                "'/opt/tomcat-dev/webapps/frog2.war'"));
        assertTrue(build.contains(
                "File configuredDevelopmentBase = file('/opt/tomcat-dev')"));
        assertTrue(build.contains(
                "if (!configuredWarPath.isAbsolute())"));
        assertTrue(build.contains(
                "if (configuredWarPath != normalizedWarPath\n"
                        + "                || normalizedWarPath != canonicalWarPath)"));
        assertTrue(build.contains(
                "!canonicalWarPath.startsWith(canonicalDevelopmentBase)"));
        assertTrue(build.contains(
                "'Development WAR must be a .war file inside /opt/tomcat-dev'"));
        assertTrue(build.contains(
                "'Development WAR path must not contain symbolic links or aliases'"));
        assertFalse(build.contains(
                "'/opt/tomcat/webapps/frog2.war'"));
        assertTrue(build.contains(
                "tasks.named('e2eSmoke') {\n"
                        + "    dependsOn verifyDevelopmentDeployment"));
        assertTrue(build.contains(
                "tasks.named('e2eAuthenticatedSmoke') {\n"
                        + "    dependsOn verifyDevelopmentDeployment"));
    }
}
