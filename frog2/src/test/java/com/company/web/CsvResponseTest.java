package com.company.web;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvResponseTest {
    @Test
    void writesBomQuotedCellsAndProtectsSpreadsheetFormulas()
            throws Exception {
        StringWriter body = new StringWriter();
        Map<String, String> headers = new LinkedHashMap<>();
        HttpServletResponse response = response(body, headers);

        CsvResponse.write(
                response,
                "customers.csv",
                List.of("고객사", "비고"),
                List.of(List.of("테스트, 고객", " =SUM(A1:A2)")));

        String csv = body.toString();
        assertEquals('\ufeff', csv.charAt(0));
        assertTrue(csv.contains("\"테스트, 고객\""));
        assertTrue(csv.contains("\"' =SUM(A1:A2)\""));
        assertEquals(
                "attachment; filename=\"customers.csv\"",
                headers.get("Content-Disposition"));
        assertEquals("no-store", headers.get("Cache-Control"));
    }

    @Test
    void rejectsUnsafeFilenameAndMismatchedRows() {
        HttpServletResponse response = response(
                new StringWriter(), new LinkedHashMap<>());

        assertThrows(IllegalArgumentException.class, () -> CsvResponse.write(
                response, "../data.csv", List.of("a"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> CsvResponse.write(
                response, "data.csv", List.of("a"), List.of(List.of("a", "b"))));
    }

    private static HttpServletResponse response(
            StringWriter body, Map<String, String> headers) {
        PrintWriter writer = new PrintWriter(body);
        return (HttpServletResponse) Proxy.newProxyInstance(
                CsvResponseTest.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWriter")) {
                        return writer;
                    }
                    if (method.getName().equals("setHeader")) {
                        headers.put((String) args[0], (String) args[1]);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }
}
