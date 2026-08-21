package com.company.controller;

import com.company.customerhistory.CustomerHistoryCategory;
import com.company.customerhistory.CustomerHistoryDraft;
import com.company.customerhistory.CustomerHistoryRecord;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.customerhistory.CustomerHistoryStatus;
import com.company.customerhistory.CustomerHistoryStorageException;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.PageResult;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.util.Pagination;
import com.company.web.ApplicationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class CustomerHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;

    private final CustomerHistoryRepository repository;
    private final CustomerDAO customerDAO;

    public CustomerHistoryServlet() {
        this(new CustomerHistoryRepository(), new CustomerDAO());
    }

    CustomerHistoryServlet(
            CustomerHistoryRepository repository,
            CustomerDAO customerDAO) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
    }

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String view = valueOrDefault(request.getParameter("view"), "list");
            switch (view) {
                case "list" -> showList(request, response, user);
                case "add" -> showAdd(request, response);
                case "edit" -> showEdit(request, response, user);
                default -> response.sendRedirect(request.getContextPath() + "/customer-history");
            }
        } catch (IllegalArgumentException exception) {
            sendBadRequest(request, response, exception.getMessage());
        } catch (CustomerHistoryStorageException exception) {
            sendStorageError(request, response);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.from(session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "add" -> add(request, response, session, user);
                case "update" -> update(request, response, session, user);
                case "delete" -> delete(request, response, session, user);
                default -> response.sendRedirect(
                        request.getContextPath() + "/customer-history");
            }
        } catch (IllegalArgumentException exception) {
            if ("add".equals(action) || "update".equals(action)) {
                showInvalidForm(request, response, action, exception.getMessage());
            } else {
                sendBadRequest(request, response, exception.getMessage());
            }
        } catch (CustomerHistoryStorageException exception) {
            sendStorageError(request, response);
        }
    }

    private void showList(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws ServletException, IOException {
        String customerName = valueOrDefault(
                request.getParameter("customerName"), "").strip();
        String category = valueOrDefault(
                request.getParameter("category"), "all").strip();
        String query = valueOrDefault(request.getParameter("q"), "").strip();
        PageResult<CustomerHistoryRecord> page = repository.findPage(
                customerName,
                category,
                query,
                Pagination.requestedPage(request.getParameter("page")),
                PAGE_SIZE);

        request.setAttribute("historyRecords", page.items());
        request.setAttribute("totalCount", page.totalCount());
        request.setAttribute("currentPage", page.page());
        request.setAttribute("totalPages", Math.max(1, page.totalPages()));
        request.setAttribute("customerName", customerName);
        request.setAttribute("category", category);
        request.setAttribute("q", query);
        request.setAttribute("currentUserId", user.getUserId());
        setReferenceData(request);
        request.getRequestDispatcher(
                "/customer-history/customer_history_list.jsp")
                .forward(request, response);
    }

    private void showAdd(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("formMode", "add");
        request.setAttribute("formWorkDate", LocalDate.now().toString());
        request.setAttribute("formStatus", CustomerHistoryStatus.COMPLETED.getCode());
        request.setAttribute("formCustomerName", valueOrDefault(
                request.getParameter("customerName"), "").strip());
        setReferenceData(request);
        forwardForm(request, response);
    }

    private void showEdit(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws ServletException, IOException {
        CustomerHistoryRecord record = repository
                .findById(request.getParameter("id"))
                .orElse(null);
        if (record == null) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "customer_history_not_found",
                    "고객사 히스토리를 찾을 수 없습니다.");
            return;
        }
        if (!record.isOwnedBy(user.getUserId())) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "customer_history_forbidden",
                    "고객사 히스토리 수정 권한이 없습니다.");
            return;
        }
        request.setAttribute("formMode", "edit");
        request.setAttribute("formId", record.getId());
        request.setAttribute("formCustomerName", record.getCustomerName());
        request.setAttribute("formWorkDate", record.getWorkDate().toString());
        request.setAttribute("formCategory", record.getCategory().getCode());
        request.setAttribute("formTitle", record.getTitle());
        request.setAttribute("formActionSummary", record.getActionSummary());
        request.setAttribute("formStatus", record.getStatus().getCode());
        setReferenceData(request);
        forwardForm(request, response);
    }

    private void add(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            UserDTO user) throws IOException {
        CustomerHistoryDraft draft = mapDraft(request);
        requireMaintenanceCustomer(draft.customerName());
        repository.create(
                draft,
                user.getUserId(),
                valueOrDefault(user.getUserName(), user.getUserId()));
        session.setAttribute("message", "고객사 히스토리가 등록되었습니다.");
        response.sendRedirect(request.getContextPath() + "/customer-history");
    }

    private void update(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            UserDTO user) throws IOException {
        CustomerHistoryDraft draft = mapDraft(request);
        requireMaintenanceCustomer(draft.customerName());
        CustomerHistoryRepository.MutationResult result = repository.updateOwned(
                request.getParameter("id"), draft, user.getUserId());
        if (result == CustomerHistoryRepository.MutationResult.FORBIDDEN) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "customer_history_forbidden",
                    "고객사 히스토리 수정 권한이 없습니다.");
            return;
        }
        if (result == CustomerHistoryRepository.MutationResult.NOT_FOUND) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "customer_history_not_found",
                    "고객사 히스토리를 찾을 수 없습니다.");
            return;
        }
        session.setAttribute("message", "고객사 히스토리가 수정되었습니다.");
        response.sendRedirect(request.getContextPath() + "/customer-history");
    }

    private void delete(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            UserDTO user) throws IOException {
        CustomerHistoryRepository.MutationResult result = repository.deleteOwned(
                request.getParameter("id"), user.getUserId());
        if (result == CustomerHistoryRepository.MutationResult.FORBIDDEN) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "customer_history_forbidden",
                    "고객사 히스토리 삭제 권한이 없습니다.");
            return;
        }
        if (result == CustomerHistoryRepository.MutationResult.NOT_FOUND) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    "customer_history_not_found",
                    "고객사 히스토리를 찾을 수 없습니다.");
            return;
        }
        session.setAttribute("message", "고객사 히스토리가 삭제되었습니다.");
        response.sendRedirect(request.getContextPath() + "/customer-history");
    }

    private void showInvalidForm(
            HttpServletRequest request,
            HttpServletResponse response,
            String action,
            String message) throws ServletException, IOException {
        request.setAttribute("formMode", "update".equals(action) ? "edit" : "add");
        request.setAttribute("formId", request.getParameter("id"));
        request.setAttribute("formCustomerName", request.getParameter("customerName"));
        request.setAttribute("formWorkDate", request.getParameter("workDate"));
        request.setAttribute("formCategory", request.getParameter("category"));
        request.setAttribute("formTitle", request.getParameter("title"));
        request.setAttribute("formActionSummary", request.getParameter("actionSummary"));
        request.setAttribute("formStatus", request.getParameter("status"));
        request.setAttribute("formError", message);
        setReferenceData(request);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forwardForm(request, response);
    }

    private CustomerHistoryDraft mapDraft(HttpServletRequest request) {
        return CustomerHistoryDraft.from(
                request.getParameter("customerName"),
                request.getParameter("workDate"),
                request.getParameter("category"),
                request.getParameter("title"),
                request.getParameter("actionSummary"),
                request.getParameter("status"));
    }

    private void requireMaintenanceCustomer(String customerName) {
        if (!customerDAO.isActiveMaintenanceCustomer(customerName)) {
            throw new IllegalArgumentException("정기점검 고객사를 선택해 주세요.");
        }
    }

    private void setReferenceData(HttpServletRequest request) {
        List<CustomerDTO> customers = customerDAO.getMaintenanceCustomers(
                "customer_name", "ASC");
        request.setAttribute("customerList", customers);
        request.setAttribute("historyCategories", CustomerHistoryCategory.values());
        request.setAttribute("historyStatuses", CustomerHistoryStatus.values());
    }

    private static void forwardForm(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(
                "/customer-history/customer_history_form.jsp")
                .forward(request, response);
    }

    private static void sendBadRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "invalid_customer_history_request",
                message);
    }

    private static void sendStorageError(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "customer_history_storage_error",
                "고객사 히스토리 저장소를 사용할 수 없습니다.");
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
