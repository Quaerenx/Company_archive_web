package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.model.TroubleshootingDTO;
import com.company.model.UserDTO;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TroubleshootingRequestMapperTest {
    private final TroubleshootingRequestMapper mapper = new TroubleshootingRequestMapper();

    @Test
    void mapsTrimmedValuesUsingTheActualFormAllowlists() {
        Map<String, String> parameters = validParameters();
        parameters.put("title", "  Connection issue  ");
        parameters.put("customer_name", "  Acme  ");
        parameters.put("note", "   ");

        TroubleshootingDTO troubleshooting = mapper.mapCreate(
                request(parameters), new UserDTO("user-1", "", "Tester", "QA"));

        assertEquals("Connection issue", troubleshooting.getTitle());
        assertEquals("Acme", troubleshooting.getCustomerName());
        assertEquals("원격", troubleshooting.getSupportType());
        assertEquals("Y", troubleshooting.getCaseOpenYn());
        assertEquals("2026-07-30",
                StrictDateParser.formatDate(troubleshooting.getOccurrenceDate()));
        assertEquals("Tester", troubleshooting.getCreator());
        assertNull(troubleshooting.getNote());
    }

    @Test
    void permitsBlankOptionalFormSelections() {
        Map<String, String> parameters = validParameters();
        parameters.put("occurrence_date", "");
        parameters.put("support_type", "");
        parameters.put("case_open_yn", "");

        TroubleshootingDTO troubleshooting =
                mapper.mapCreate(request(parameters), user());

        assertNull(troubleshooting.getOccurrenceDate());
        assertNull(troubleshooting.getSupportType());
        assertNull(troubleshooting.getCaseOpenYn());
    }

    @Test
    void rejectsInvalidIdDateRequiredFieldsAndValuesOutsideTheFormAllowlists() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.positiveInt(request(Map.of("id", "0")), "id"));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapUpdate(request(validParameters())));

        Map<String, String> invalidDate = validParameters();
        invalidDate.put("occurrence_date", "2026-02-30");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(invalidDate), user()));

        Map<String, String> invalidSupportType = validParameters();
        invalidSupportType.put("support_type", "전화");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(invalidSupportType), user()));

        Map<String, String> invalidCaseValue = validParameters();
        invalidCaseValue.put("case_open_yn", "UNKNOWN");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(invalidCaseValue), user()));

        Map<String, String> blankCustomer = validParameters();
        blankCustomer.put("customer_name", " ");
        assertThrows(IllegalArgumentException.class,
                () -> mapper.mapCreate(request(blankCustomer), user()));
    }

    private static Map<String, String> validParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("title", "Connection issue");
        parameters.put("customer_name", "Acme");
        parameters.put("occurrence_date", "2026-07-30");
        parameters.put("support_type", "원격");
        parameters.put("case_open_yn", "Y");
        return parameters;
    }

    private static UserDTO user() {
        return new UserDTO("user-1", "", "Tester", "QA");
    }

    private static HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> {
                    if ("getParameter".equals(call.getName())) {
                        return parameters.get((String) args[0]);
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
