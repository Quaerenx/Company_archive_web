package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDTO;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MyPageRequestMapperTest {
    private final MyPageRequestMapper mapper = new MyPageRequestMapper();

    @Test
    void trimsProfileNameAndRejectsMissingOrOversizedValues() {
        assertEquals("테스터", mapper.profileName(
                request(Map.of("userName", "  테스터  "))));

        IllegalArgumentException blank = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.profileName(request(Map.of("userName", "  "))));
        assertEquals("이름을 입력해 주세요.", blank.getMessage());

        IllegalArgumentException oversized = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.profileName(request(Map.of(
                        "userName", "가".repeat(101)))));
        assertEquals(
                "이름은 100자 이하로 입력해 주세요.",
                oversized.getMessage());
    }

    @Test
    void mapsOwnedMonthlyResponseAndPreservesOptionalText() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("responseId", " 17 ");
        parameters.put("responseDate", "2026-08-30");
        parameters.put("customerName", "  고객사  ");
        parameters.put("reason", "  기술지원  ");
        parameters.put("actionContent", "  조치 내용  ");

        MonthlyCustomerResponseDTO response = mapper.monthlyResponse(
                request(parameters),
                new UserDTO("user-1", "", "테스터", "QA"),
                true);

        assertEquals(17, response.getId());
        assertEquals("user-1", response.getUserId());
        assertEquals("테스터", response.getUserName());
        assertEquals(
                "2026-08-30",
                StrictDateParser.formatDate(response.getResponseDate()));
        assertEquals("고객사", response.getCustomerName());
        assertEquals("기술지원", response.getReason());
        assertEquals("  조치 내용  ", response.getActionContent());
        assertNull(response.getNote());
    }

    @Test
    void rejectsInvalidMonthlyFieldsBeforeAnyCommandRuns() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("responseDate", "2026-02-30");
        parameters.put("customerName", "고객사");
        parameters.put("reason", "기술지원");

        IllegalArgumentException invalidDate = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.monthlyResponse(
                        request(parameters), user(), false));
        assertEquals(
                "날짜 형식이 올바르지 않습니다.",
                invalidDate.getMessage());

        parameters.put("responseDate", "2026-08-30");
        parameters.put("reason", "  ");
        IllegalArgumentException missingRequired = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.monthlyResponse(
                        request(parameters), user(), false));
        assertEquals(
                "날짜, 고객명, 사유를 모두 입력해 주세요.",
                missingRequired.getMessage());
    }

    @Test
    void mapsMonthSelectionWithTheExistingFallbackContract() {
        YearMonth fallback = YearMonth.of(2026, 8);

        assertEquals(
                new MyPageRequestMapper.MonthSelection(2026, 8, false),
                mapper.monthSelection(request(Map.of()), fallback));
        assertEquals(
                new MyPageRequestMapper.MonthSelection(2025, 7, true),
                mapper.monthSelection(request(Map.of(
                        "year", "2025", "month", "7")), fallback));
        assertEquals(
                new MyPageRequestMapper.MonthSelection(2025, 8, true),
                mapper.monthSelection(request(Map.of(
                        "year", "2025", "month", "invalid")), fallback));
    }

    private static UserDTO user() {
        return new UserDTO("user-1", "", "테스터", "QA");
    }

    private static HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, arguments) -> {
                    if ("getParameter".equals(call.getName())) {
                        return parameters.get((String) arguments[0]);
                    }
                    return defaultValue(call.getReturnType());
                });
    }
}
