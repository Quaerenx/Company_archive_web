package com.company.controller;

import com.company.model.TroubleshootingDTO;
import com.company.model.UserDTO;
import com.company.util.Pagination;
import com.company.util.StrictDateParser;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

final class TroubleshootingRequestMapper {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final Set<String> SUPPORT_TYPES = Set.of("방문", "원격");
    private static final Set<String> CASE_OPEN_VALUES = Set.of("Y", "N");

    TroubleshootingDTO mapCreate(HttpServletRequest request, UserDTO user) {
        TroubleshootingDTO troubleshooting = mapForm(request);
        troubleshooting.setCreatorUserId(requiredIdentity(
                user == null ? null : user.getUserId()));
        troubleshooting.setCreator(requiredIdentity(
                user == null ? null : user.getUserName()));
        return troubleshooting;
    }

    TroubleshootingDTO mapUpdate(HttpServletRequest request) {
        int id = positiveInt(request, "id");
        TroubleshootingDTO troubleshooting = mapForm(request);
        troubleshooting.setId(id);
        return troubleshooting;
    }

    int positiveInt(HttpServletRequest request, String parameterName) {
        String value = trimmed(request.getParameter(parameterName));
        if (value == null) {
            throw invalid("트러블 슈팅 ID가 필요합니다.");
        }
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw invalid("트러블 슈팅 ID가 올바르지 않습니다.");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw invalid("트러블 슈팅 ID가 올바르지 않습니다.");
        }
    }

    int requestedPage(HttpServletRequest request) {
        return Pagination.requestedPage(request.getParameter("page"));
    }

    int requestedPageSize(HttpServletRequest request) {
        return Pagination.requestedPageSize(
                request.getParameter("pageSize"),
                DEFAULT_PAGE_SIZE,
                MAXIMUM_PAGE_SIZE);
    }

    private TroubleshootingDTO mapForm(HttpServletRequest request) {
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setTitle(required(request, "title", "제목을 입력해주세요."));
        troubleshooting.setCustomerName(
                required(request, "customer_name", "고객사를 선택해주세요."));
        troubleshooting.setCustomerManager(optional(request, "customer_manager"));
        troubleshooting.setOccurrenceDate(optionalDate(request.getParameter("occurrence_date")));
        troubleshooting.setWorkPersonnel(optional(request, "work_personnel"));
        troubleshooting.setWorkPeriod(optional(request, "work_period"));
        troubleshooting.setSupportType(allowlistedOptional(
                request, "support_type", SUPPORT_TYPES, "지원형태가 올바르지 않습니다."));
        troubleshooting.setCaseOpenYn(allowlistedOptional(
                request, "case_open_yn", CASE_OPEN_VALUES,
                "케이스오픈 여부가 올바르지 않습니다."));
        troubleshooting.setOverview(optional(request, "overview"));
        troubleshooting.setCauseAnalysis(optional(request, "cause_analysis"));
        troubleshooting.setErrorContent(optional(request, "error_content"));
        troubleshooting.setActionTaken(optional(request, "action_taken"));
        troubleshooting.setScriptContent(optional(request, "script_content"));
        troubleshooting.setNote(optional(request, "note"));
        return troubleshooting;
    }

    private static Date optionalDate(String value) {
        String normalized = trimmed(value);
        if (normalized == null) {
            return null;
        }
        try {
            return StrictDateParser.parseDate(normalized);
        } catch (ParseException exception) {
            throw invalid("발생일자가 올바르지 않습니다.");
        }
    }

    private static String allowlistedOptional(
            HttpServletRequest request,
            String parameterName,
            Set<String> allowedValues,
            String message) {
        String value = optional(request, parameterName);
        if (value != null && !allowedValues.contains(value)) {
            throw invalid(message);
        }
        return value;
    }

    private static String required(
            HttpServletRequest request, String parameterName, String message) {
        String value = optional(request, parameterName);
        if (value == null) {
            throw invalid(message);
        }
        return value;
    }

    private static String optional(HttpServletRequest request, String parameterName) {
        return trimmed(request.getParameter(parameterName));
    }

    private static String trimmed(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String requiredIdentity(String value) {
        String identity = trimmed(value);
        if (identity == null) {
            throw invalid("로그인 사용자 정보를 확인할 수 없습니다.");
        }
        return identity;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
