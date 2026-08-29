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
import com.company.util.BusinessDate;
import com.company.web.ApplicationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class CustomerHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 20;

    private final CustomerHistoryRepository repository;
    private final CustomerDAO customerDAO;
    private final Clock clock;

    public CustomerHistoryServlet() {
        this(new CustomerHistoryRepository(), new CustomerDAO(),
                BusinessDate.systemClock());
    }

    CustomerHistoryServlet(
            CustomerHistoryRepository repository,
            CustomerDAO customerDAO) {
        this(repository, customerDAO, BusinessDate.systemClock());
    }

    CustomerHistoryServlet(
            CustomerHistoryRepository repository,
            CustomerDAO customerDAO,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        FlashMessage.expose(request);

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
                case "add" -> add(request, response, user);
                case "update" -> update(request, response, user);
                case "delete" -> delete(request, response, user);
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
        request.setAttribute(
                "hasActiveFilters",
                !customerName.isEmpty()
                        || (!category.isEmpty() && !"all".equalsIgnoreCase(category))
                        || !query.isEmpty());
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
        request.setAttribute(
                "formWorkDate", BusinessDate.today(clock).toString());
        request.setAttribute("formStatus", CustomerHistoryStatus.COMPLETED.getCode());
        request.setAttribute("formCustomerName", valueOrDefault(
                request.getParameter("customerName"), "").strip());
        exposeListReturnState(request);
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
        exposeListReturnState(request);
        setReferenceData(request);
        forwardForm(request, response);
    }

    private void add(
            HttpServletRequest request,
            HttpServletResponse response,
            UserDTO user) throws IOException {
        CustomerHistoryDraft draft = mapDraft(request);
        requireMaintenanceCustomer(draft.customerName());
        repository.create(
                draft,
                user.getUserId(),
                valueOrDefault(user.getUserName(), user.getUserId()));
        redirectToList(request, response, "고객사 히스토리가 등록되었습니다.");
    }

    private void update(
            HttpServletRequest request,
            HttpServletResponse response,
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
        redirectToList(request, response, "고객사 히스토리가 수정되었습니다.");
    }

    private void delete(
            HttpServletRequest request,
            HttpServletResponse response,
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
        redirectToList(request, response, "고객사 히스토리가 삭제되었습니다.");
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
        exposeListReturnState(request);
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

    private static void exposeListReturnState(HttpServletRequest request) {
        ListReturnState state = ListReturnState.from(request);
        request.setAttribute("formReturnCustomerName", state.customerName());
        request.setAttribute("formReturnCategory", state.category());
        request.setAttribute("formReturnQ", state.query());
        request.setAttribute("formReturnPage", state.page());
        request.setAttribute("returnListUrl", state.toUrl(request.getContextPath()));
    }

    private static void redirectToList(
            HttpServletRequest request,
            HttpServletResponse response,
            String message) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                ListReturnState.from(request).toUrl(request.getContextPath()),
                message,
                "success");
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

    private record ListReturnState(
            String customerName,
            String category,
            String query,
            int page) {
        private static ListReturnState from(HttpServletRequest request) {
            return new ListReturnState(
                    normalizeText(
                            request.getParameter("returnCustomerName"),
                            CustomerHistoryDraft.MAX_CUSTOMER_NAME_LENGTH),
                    normalizeCategory(request.getParameter("returnCategory")),
                    normalizeText(request.getParameter("returnQ"), 100),
                    Pagination.requestedPage(request.getParameter("returnPage")));
        }

        private String toUrl(String contextPath) {
            StringBuilder url = new StringBuilder(contextPath)
                    .append("/customer-history");
            appendParameter(url, "customerName", customerName);
            if (!"all".equals(category)) {
                appendParameter(url, "category", category);
            }
            appendParameter(url, "q", query);
            if (page > 1) {
                appendParameter(url, "page", Integer.toString(page));
            }
            return url.toString();
        }

        private static String normalizeCategory(String value) {
            String normalized = valueOrDefault(value, "all").strip();
            if (normalized.isEmpty() || "all".equalsIgnoreCase(normalized)) {
                return "all";
            }
            try {
                return CustomerHistoryCategory.fromCode(normalized).getCode();
            } catch (IllegalArgumentException exception) {
                return "all";
            }
        }

        private static String normalizeText(String value, int maximumLength) {
            String normalized = valueOrDefault(value, "").strip();
            return normalized.length() <= maximumLength ? normalized : "";
        }

        private static void appendParameter(
                StringBuilder url, String name, String value) {
            if (value.isEmpty()) {
                return;
            }
            url.append(url.indexOf("?") < 0 ? '?' : '&')
                    .append(name)
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }
}
