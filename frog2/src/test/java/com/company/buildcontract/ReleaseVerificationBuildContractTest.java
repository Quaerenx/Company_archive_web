package com.company.buildcontract;

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
                "'Production WAR cannot be used for development E2E verification'"));
        assertTrue(build.contains(
                "tasks.named('e2eSmoke') {\n"
                        + "    dependsOn verifyDevelopmentDeployment"));
        assertTrue(build.contains(
                "tasks.named('e2eAuthenticatedSmoke') {\n"
                        + "    dependsOn verifyDevelopmentDeployment"));
    }
}
