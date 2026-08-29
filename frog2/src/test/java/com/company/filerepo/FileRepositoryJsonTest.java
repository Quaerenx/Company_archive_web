package com.company.filerepo;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.filerepo.FileRepositoryService.StoredFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FileRepositoryJsonTest {
    @Test
    void preservesCreatedResponseShapeAndEscapesFileValues() throws Exception {
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);
        AtomicInteger status = new AtomicInteger();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        status.set((Integer) args[0]);
                        yield null;
                    }
                    case "getWriter" -> writer;
                    default -> defaultValue(method.getReturnType());
                });

        FileRepositoryJson.sendCreated(
                response,
                List.of(new StoredFile("", "id\"\\\n", "report\t.txt", 7L)));
        writer.flush();

        assertEquals(HttpServletResponse.SC_CREATED, status.get());
        assertEquals(
                "{\"status\":\"ok\",\"success\":true,\"files\":["
                        + "{\"id\":\"id\\\"\\\\\\n\","
                        + "\"name\":\"report\\t.txt\",\"size\":7}]}",
                body.toString());
    }
}
