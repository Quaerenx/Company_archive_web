package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.filerepo.FileRepositoryService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryDownloadServletTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rethrowsStreamingFailureAfterResponseIsCommitted() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        FileRepositoryService service = new FileRepositoryService(root);
        byte[] content = "safe repository content".getBytes(StandardCharsets.UTF_8);
        var validated = service.validateUpload("safe.txt", "text/plain", content.length);
        var stored = service.store("", validated, content.length, new ByteArrayInputStream(content));
        FileRepositoryDownloadServlet servlet = servletUsing(service);

        assertThrows(IOException.class, () -> servlet.doGet(
                requestFor(stored.id()),
                committedResponseWithFailingStream()));
    }

    private static FileRepositoryDownloadServlet servletUsing(FileRepositoryService service) throws Exception {
        FileRepositoryDownloadServlet servlet = new FileRepositoryDownloadServlet();
        Field field = FileRepositoryDownloadServlet.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(servlet, service);
        return servlet;
    }

    private static HttpServletRequest requestFor(String id) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                FileRepositoryDownloadServletTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    if ("getParameter".equals(method.getName())) {
                        return "id".equals(args[0]) ? id : "";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static HttpServletResponse committedResponseWithFailingStream() {
        ServletOutputStream output = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                // Synchronous test stream.
            }

            @Override
            public void write(int value) throws IOException {
                throw new IOException("simulated client disconnect");
            }
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                FileRepositoryDownloadServletTest.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "isCommitted" -> true;
                    case "getOutputStream" -> output;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
