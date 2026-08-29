package com.company.buildcontract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JspcJava22BuildContractTest {
    private static final Path BUILD_FILE = Path.of("build.gradle");
    private static final Path JSPC_BUILD_FILE = Path.of("gradle/jspc-java22.gradle");

    @Test
    void checkCompilesGeneratedJspSourcesWithTheJava22Toolchain() throws Exception {
        String build = Files.readString(BUILD_FILE);
        assertTrue(
                build.contains("apply from: 'gradle/jspc-java22.gradle'"),
                "The Java 22 JspC verification must be part of the Gradle build");

        assertTrue(Files.isRegularFile(JSPC_BUILD_FILE),
                "Missing build-scoped JspC verification script");
        String jspcBuild = Files.readString(JSPC_BUILD_FILE);

        assertTrue(jspcBuild.contains("tasks.register('generateJspSources', JavaExec)"));
        assertTrue(jspcBuild.contains("mainClass = 'org.apache.jasper.JspC'"));
        assertFalse(
                jspcBuild.contains("/opt/tomcat-dev-home/current"),
                "JspC must not depend on a development-server path");
        assertTrue(jspcBuild.contains(
                "environmentVariable('FROG2_JSPC_CATALINA_HOME')"));
        assertTrue(jspcBuild.contains(
                "environmentVariable('FROG2_JSPC_JASPER_VERSION')"));
        assertTrue(jspcBuild.contains("orElse('10.1.59')"));
        assertFalse(
                jspcBuild.contains("environmentVariable('CATALINA_HOME')"),
                "JspC must not silently select the production Tomcat from CATALINA_HOME");
        assertTrue(jspcBuild.contains(
                "JspC validation requires frog2JspcCatalinaHome or"));
        assertTrue(jspcBuild.contains("implementationVersion != expectedJasperVersion.get()"));
        assertTrue(jspcBuild.contains("tasks.register('compileJspJava22', JavaCompile)"));
        assertTrue(jspcBuild.contains("reproducibleSourceTimestamp = 315532800000L"));
        assertTrue(jspcBuild.contains("setLastModified(reproducibleSourceTimestamp)"));
        assertTrue(jspcBuild.contains("include '**/*.jsp'"));
        assertTrue(jspcBuild.contains("'-threadCount', '1'"));
        assertTrue(jspcBuild.contains("javaToolchains.compilerFor"));
        assertTrue(jspcBuild.contains("options.release = 22"));
        assertTrue(jspcBuild.contains("tasks.named('check')"));
        assertTrue(jspcBuild.contains("dependsOn tasks.named('jspcJava22')"));
        assertTrue(jspcBuild.contains("tasks.register('normalizeJspSources')"));
        assertTrue(jspcBuild.contains("dependsOn normalizeJspSources"));
        assertTrue(jspcBuild.contains("Cannot normalize TLD dependency"));
        assertTrue(jspcBuild.contains("inputs.file(jspcPreparedWebXml)"));
        assertTrue(jspcBuild.contains("File sourceWebXml = jspcPreparedWebXml.get().asFile"));
        assertTrue(jspcBuild.contains("into 'WEB-INF/classes'"));
        assertTrue(jspcBuild.contains("webXml = jspcMergedWebXml.get().asFile"));
        assertTrue(build.contains("def packagedWar = tasks.named('war')"));
        assertTrue(build.contains("archiveFileName = 'frog2.war'"));
        assertTrue(build.contains("mappedJspClasses.isEmpty()"));
        assertTrue(build.contains("precompiledJspClasses.size() != mappedJspClasses.size()"));
        assertTrue(build.contains("missingPrecompiledJspClasses.isEmpty()"));
        assertFalse(
                jspcBuild.contains("'-compile'"),
                "Tomcat's bundled ECJ must not silently compile JSPs as Java 19");
    }

}
