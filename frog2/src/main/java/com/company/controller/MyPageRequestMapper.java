package com.company.controller;

import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDTO;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.time.YearMonth;

final class MyPageRequestMapper {
    private static final int USER_NAME_MAX_LENGTH = 100;
    private static final String REQUIRED_MONTHLY_RESPONSE_MESSAGE =
            "날짜, 고객명, 사유를 모두 입력해 주세요.";

    String profileName(HttpServletRequest request) {
        String userName = trimmed(request.getParameter("userName"));
        if (userName == null) {
            throw invalid("이름을 입력해 주세요.");
        }
        if (userName.length() > USER_NAME_MAX_LENGTH) {
            throw invalid("이름은 100자 이하로 입력해 주세요.");
        }
        return userName;
    }

    MyPageCommandService.PasswordChange passwordChange(
            HttpServletRequest request) {
        return new MyPageCommandService.PasswordChange(
                request.getParameter("currentPassword"),
                request.getParameter("newPassword"),
                request.getParameter("confirmPassword"));
    }

    MonthlyCustomerResponseDTO monthlyResponse(
            HttpServletRequest request,
            UserDTO user,
            boolean requireId) {
        String responseDateValue = requiredMonthlyValue(
                request.getParameter("responseDate"));
        String customerName = requiredMonthlyValue(
                request.getParameter("customerName"));
        String reason = requiredMonthlyValue(request.getParameter("reason"));

        MonthlyCustomerResponseDTO response = new MonthlyCustomerResponseDTO();
        if (requireId) {
            response.setId(positiveResponseId(
                    request.getParameter("responseId"),
                    "입력값이 올바르지 않습니다."));
        }
        response.setUserId(user.getUserId());
        response.setUserName(user.getUserName());
        try {
            response.setResponseDate(
                    StrictDateParser.parseDate(responseDateValue));
        } catch (ParseException exception) {
            throw invalid("날짜 형식이 올바르지 않습니다.");
        }
        response.setCustomerName(customerName);
        response.setReason(reason);
        response.setActionContent(request.getParameter("actionContent"));
        response.setNote(request.getParameter("note"));
        return response;
    }

    int responseIdForDelete(HttpServletRequest request) {
        return positiveResponseId(
                request.getParameter("responseId"), "잘못된 요청입니다.");
    }

    MonthSelection monthSelection(
            HttpServletRequest request, YearMonth defaultMonth) {
        String yearValue = request.getParameter("year");
        String monthValue = request.getParameter("month");
        int year = defaultMonth.getYear();
        int month = defaultMonth.getMonthValue();
        if (yearValue != null && monthValue != null) {
            try {
                year = Integer.parseInt(yearValue);
                month = Integer.parseInt(monthValue);
            } catch (NumberFormatException ignored) {
                // Preserve the legacy partial fallback when only one value
                // can be parsed.
            }
        }
        return new MonthSelection(
                year,
                month,
                yearValue != null && monthValue != null);
    }

    private static int positiveResponseId(String value, String message) {
        String normalized = trimmed(value);
        if (normalized == null) {
            throw invalid(message);
        }
        try {
            int id = Integer.parseInt(normalized);
            if (id <= 0) {
                throw invalid(message);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw invalid(message);
        }
    }

    private static String requiredMonthlyValue(String value) {
        String normalized = trimmed(value);
        if (normalized == null) {
            throw invalid(REQUIRED_MONTHLY_RESPONSE_MESSAGE);
        }
        return normalized;
    }

    private static String trimmed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    record MonthSelection(int year, int month, boolean explicitlySelected) {
    }
}
