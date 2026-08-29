package com.company.web;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class JsonResponseTest {
    @Test
    void encodesQuotedAndNullableStringsWithoutChangingNullSemantics() {
        String value = "a\"b\\c\n\b\f\u0001";

        assertEquals("\"a\\\"b\\\\c\\n\\b\\f\\u0001\"", JsonResponse.string(value));
        assertEquals("\"\"", JsonResponse.string(null));
        assertEquals("null", JsonResponse.nullableString(null));
        assertEquals(JsonResponse.string(value), JsonResponse.nullableString(value));
    }

    @Test
    void preservesLegacyEmptyStringsForNullErrorFields() throws Exception {
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "getWriter" -> writer;
                    default -> defaultValue(method.getReturnType());
                });

        JsonResponse.sendError(
                response, HttpServletResponse.SC_BAD_REQUEST, null, null);
        writer.flush();

        assertEquals(
                "{\"status\":\"error\",\"success\":false,"
                        + "\"code\":\"\",\"message\":\"\"}",
                body.toString());
    }
}
