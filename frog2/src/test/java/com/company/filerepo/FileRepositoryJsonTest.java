package com.company.filerepo;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.filerepo.FileRepositoryService.ImportItem;
import com.company.filerepo.FileRepositoryService.ImportResult;
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
        AtomicInteger status = new AtomicInteger();
        HttpServletResponse response = response(body, status);

        FileRepositoryJson.sendCreated(
                response,
                List.of(new StoredFile("", "id\"\\\n", "report\t.txt", 7L)));

        assertEquals(HttpServletResponse.SC_CREATED, status.get());
        assertEquals(
                "{\"status\":\"ok\",\"success\":true,\"files\":["
                        + "{\"id\":\"id\\\"\\\\\\n\","
                        + "\"name\":\"report\\t.txt\",\"size\":7}]}",
                body.toString());
    }

    @Test
    void importResultIncludesPerFileStatusAndRetryability() throws Exception {
        StringWriter body = new StringWriter();
        AtomicInteger status = new AtomicInteger();
        HttpServletResponse response = response(body, status);
        ImportResult result = new ImportResult("rpm", List.of(
                new ImportItem(
                        "rpm/ok.rpm",
                        "ok.rpm",
                        "imported",
                        "등록 완료",
                        "자료실에 등록했습니다.",
                        false),
                new ImportItem(
                        "rpm/retry.rpm",
                        "retry.rpm",
                        "failed",
                        "반입 실패",
                        "다시 시도",
                        false)));

        FileRepositoryJson.sendImportResult(
                response, result, "/frog2/file-repository?path=rpm");

        assertEquals(HttpServletResponse.SC_OK, status.get());
        assertTrue(body.toString().contains("\"imported\":1"));
        assertTrue(body.toString().contains("\"failed\":1"));
        assertTrue(body.toString().contains("\"retryable\":true"));
        assertTrue(body.toString().contains("rpm/retry.rpm"));
    }

    private static HttpServletResponse response(
            StringWriter body, AtomicInteger status) {
        PrintWriter writer = new PrintWriter(body);
        return (HttpServletResponse) Proxy.newProxyInstance(
                FileRepositoryJsonTest.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        status.set((Integer) args[0]);
                        yield null;
                    }
                    case "getWriter" -> writer;
                    default -> defaultValue(method.getReturnType());
                });
    }
}
