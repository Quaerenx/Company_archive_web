package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.util.StrictDateParser;
import com.company.web.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Objects;

final class CustomerCommandController {
    private final CustomerCommandService service;
    private final CustomerRequestMapper mapper;

    CustomerCommandController() {
        this(new CustomerCommandService(), new CustomerRequestMapper());
    }

    CustomerCommandController(CustomerCommandService service, CustomerRequestMapper mapper) {
        this.service = Objects.requireNonNull(service, "service");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        String action = request.getParameter("action");
        UserDTO principal = SessionPrincipal.from(session);
        String actorUserId = principal == null ? null : principal.getUserId();
        if ("saveDetail".equals(action)) {
            saveDetail(request, response, actorUserId);
            return;
        }

        switch (action == null ? "" : action) {
            case "update" -> updateCustomer(
                    request, response, actorUserId);
            case "add" -> addCustomer(
                    request, response, actorUserId);
            case "delete" -> deleteCustomer(
                    request, response, actorUserId);
            default -> response.sendRedirect("customers?view=list");
        }
    }

    private void updateCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            String actorUserId) throws IOException {
        if (rejectInvalidEosDate(
                request, response, "customers?view=list")) {
            return;
        }
        CustomerDTO customer = mapper.mapCustomer(request);
        redirectWithResult(
                request,
                response,
                "customers?view=list",
                service.updateCustomer(customer, actorUserId),
                "고객사 정보가 성공적으로 업데이트되었습니다.",
                "고객사 정보 업데이트 중 오류가 발생했습니다.");
    }

    private void addCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            String actorUserId) throws IOException {
        if (rejectInvalidEosDate(
                request, response, "customers?view=add")) {
            return;
        }
        CustomerDTO customer = mapper.mapCustomer(request);
        redirectWithResult(
                request,
                response,
                "customers?view=list",
                service.addCustomer(customer, actorUserId),
                "새 고객사가 성공적으로 추가되었습니다.",
                "고객사 추가 중 오류가 발생했습니다.");
    }

    private void deleteCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            String actorUserId) throws IOException {
        redirectWithResult(
                request,
                response,
                "customers?view=list",
                service.deleteCustomer(
                        request.getParameter("customer_name"), actorUserId),
                "고객사가 성공적으로 삭제되었습니다.",
                "고객사 삭제 중 오류가 발생했습니다.");
    }

    private void saveDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            String actorUserId) throws IOException {
        CustomerEnvironment environment;
        try {
            environment = mapper.environment(request);
        } catch (IllegalArgumentException exception) {
            rejectDetailSave(
                    request,
                    response,
                    null,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_environment",
                    "고객사 환경 값이 올바르지 않습니다.");
            return;
        }

        try {
            CustomerDetailDTO detail = mapper.mapCustomerDetail(request);
            if (detail.getCustomerName() == null) {
                rejectDetailSave(
                        request,
                        response,
                        environment,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "missing_customer_name",
                        "고객사명이 필요합니다.");
                return;
            }
            boolean success = service.saveCustomerDetail(
                    environment, detail, actorUserId);

            String encodedName = URLEncoder.encode(
                    detail.getCustomerName(), StandardCharsets.UTF_8);
            redirectWithResult(
                    request,
                    response,
                    "customers?view=detail&customerName=" + encodedName
                            + "&env=" + environment.externalValue(),
                    success,
                    "상세정보가 성공적으로 저장되었습니다.",
                    "상세정보 저장 중 오류가 발생했습니다.");
        } catch (ParseException exception) {
            rejectDetailSave(
                    request,
                    response,
                    environment,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_date",
                    "날짜 형식이 올바르지 않습니다.");
        }
    }

    private static void rejectDetailSave(
            HttpServletRequest request,
            HttpServletResponse response,
            CustomerEnvironment environment,
            int status,
            String code,
            String message) throws IOException {
        if (JsonResponse.isExpected(request)) {
            JsonResponse.sendError(response, status, code, message);
            return;
        }

        String customerName = request.getParameter("customerName");
        String location = "customers?view=list";
        if (customerName != null && !customerName.trim().isEmpty()) {
            String encodedName = URLEncoder.encode(
                    customerName.trim(), StandardCharsets.UTF_8);
            String targetEnvironment = environment == null
                    ? CustomerEnvironment.PROD.externalValue()
                    : environment.externalValue();
            location = "customers?view=editDetail&customerName="
                    + encodedName + "&env=" + targetEnvironment;
        }
        FlashMessage.redirect(
                request, response, location, message, "error");
    }

    private static void redirectWithResult(
            HttpServletRequest request,
            HttpServletResponse response,
            String location,
            boolean success,
            String successMessage,
            String errorMessage) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                location,
                success ? successMessage : errorMessage,
                success ? "success" : "error");
    }

    private static boolean rejectInvalidEosDate(
            HttpServletRequest request,
            HttpServletResponse response,
            String location) throws IOException {
        String value = request.getParameter("vertica_eos");
        if (value == null || value.isBlank()
                || StrictDateParser.parseSqlDateOrNull(value) != null) {
            return false;
        }
        FlashMessage.redirect(
                request,
                response,
                location,
                "EOS 날짜 형식이 올바르지 않습니다.",
                "error");
        return true;
    }
}
