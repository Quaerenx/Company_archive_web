package com.company;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.company.config.ApplicationEnvironment;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeClassLoadingTest {
    @Test
    void allProductionClassesLinkWithoutInitialization() throws Exception {
        URI classesUri = ApplicationEnvironment.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path classesRoot = Path.of(classesUri);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        List<String> classNames;
        try (var paths = Files.walk(classesRoot)) {
            classNames = paths
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .map(classesRoot::relativize)
                    .map(Path::toString)
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .map(name -> name.replace('/', '.'))
                    .sorted()
                    .toList();
        }

        for (String className : classNames) {
            assertDoesNotThrow(() -> Class.forName(className, false, classLoader), className);
        }
    }
}
