package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GetRequestDatabasePurityContractTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path CONTROLLERS =
            MAIN_JAVA.resolve("com/company/controller");
    private static final Pattern MUTATING_CALL = Pattern.compile(
            "\\.(?:add|create|delete|increment|insert|save|update|upsert)"
                    + "[A-Z][A-Za-z0-9_]*\\s*\\(");
    private static final Pattern RUNTIME_DDL = Pattern.compile(
            "(?i)\\b(?:CREATE|ALTER|DROP|TRUNCATE)\\s+(?:TABLE|COLUMN)\\b");

    @Test
    void getHandlersContainNoKnownMutationCalls() throws Exception {
        List<String> violations = new ArrayList<>();
        try (var files = Files.list(CONTROLLERS)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().endsWith("Servlet.java"))
                    .sorted()
                    .toList()) {
                String source = Files.readString(file);
                int start = source.indexOf("void doGet(");
                if (start < 0) {
                    continue;
                }
                int end = source.indexOf("void doPost(", start);
                String getSection = source.substring(
                        start, end < 0 ? source.length() : end);
                Matcher matcher = MUTATING_CALL.matcher(getSection);
                while (matcher.find()) {
                    violations.add(file.getFileName() + ": " + matcher.group());
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "GET mutation calls: " + violations);
    }

    @Test
    void runtimeJavaContainsNoDatabaseDdl() throws Exception {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(MAIN_JAVA)) {
            for (Path file : files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                String source = Files.readString(file);
                if (RUNTIME_DDL.matcher(source.toUpperCase(Locale.ROOT)).find()) {
                    violations.add(MAIN_JAVA.relativize(file).toString());
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Runtime DDL sources: " + violations);
    }
}
