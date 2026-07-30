package com.company.controller;

import com.company.model.CustomerDTO;
import com.company.model.CustomerDetailDTO;
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
        if ("saveDetail".equals(action)) {
            saveDetail(request, response, session);
            return;
        }

        switch (action == null ? "" : action) {
            case "update" -> updateCustomer(request, response, session);
            case "add" -> addCustomer(request, response, session);
            case "delete" -> deleteCustomer(request, response, session);
            default -> response.sendRedirect("customers?view=list");
        }
    }

    private void updateCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        CustomerDTO customer = mapper.mapCustomer(request);
        setResultMessage(
                session,
                service.updateCustomer(customer),
                "고객사 정보가 성공적으로 업데이트되었습니다.",
                "고객사 정보 업데이트 중 오류가 발생했습니다.");
        response.sendRedirect("customers?view=list");
    }

    private void addCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        CustomerDTO customer = mapper.mapCustomer(request);
        setResultMessage(
                session,
                service.addCustomer(customer),
                "새 고객사가 성공적으로 추가되었습니다.",
                "고객사 추가 중 오류가 발생했습니다.");
        response.sendRedirect("customers?view=list");
    }

    private void deleteCustomer(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        setResultMessage(
                session,
                service.deleteCustomer(request.getParameter("customer_name")),
                "고객사가 성공적으로 삭제되었습니다.",
                "고객사 삭제 중 오류가 발생했습니다.");
        response.sendRedirect("customers?view=list");
    }

    private void saveDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        CustomerEnvironment environment;
        try {
            environment = mapper.environment(request);
        } catch (IllegalArgumentException exception) {
            JsonResponse.sendError(
                    response, HttpServletResponse.SC_BAD_REQUEST, "invalid_environment",
                    "고객사 환경 값이 올바르지 않습니다.");
            return;
        }

        try {
            CustomerDetailDTO detail = mapper.mapCustomerDetail(request);
            setResultMessage(
                    session,
                    service.saveCustomerDetail(environment, detail),
                    "상세정보가 성공적으로 저장되었습니다.",
                    "상세정보 저장 중 오류가 발생했습니다.");

            String encodedName = URLEncoder.encode(
                    detail.getCustomerName(), StandardCharsets.UTF_8);
            response.sendRedirect("customers?view=detail&customerName=" + encodedName
                    + "&env=" + environment.externalValue());
        } catch (ParseException exception) {
            JsonResponse.sendError(
                    response, HttpServletResponse.SC_BAD_REQUEST, "invalid_date",
                    "날짜 형식이 올바르지 않습니다.");
        }
    }

    private static void setResultMessage(
            HttpSession session,
            boolean success,
            String successMessage,
            String errorMessage) {
        session.setAttribute(success ? "message" : "error", success ? successMessage : errorMessage);
    }
}
