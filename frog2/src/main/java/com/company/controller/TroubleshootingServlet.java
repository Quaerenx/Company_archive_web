package com.company.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.company.model.CustomerDAO;
import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import com.company.util.SearchQueryPolicy;
import com.company.web.ApplicationError;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/troubleshooting") - web.xml에서 매핑하므로 주석 처리
public class TroubleshootingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final TroubleshootingRequestMapper requestMapper =
            new TroubleshootingRequestMapper();
    private final TroubleshootingDAO troubleshootingDAO;
    private final CustomerDAO customerDAO;
    private final CustomerAssignmentDAO customerAssignmentDAO;

    public TroubleshootingServlet() {
        this(new TroubleshootingDAO(), new CustomerDAO(),
                new CustomerAssignmentDAO());
    }

    TroubleshootingServlet(TroubleshootingDAO troubleshootingDAO) {
        this(troubleshootingDAO, new CustomerDAO(),
                new CustomerAssignmentDAO());
    }

    TroubleshootingServlet(
            TroubleshootingDAO troubleshootingDAO,
            CustomerDAO customerDAO) {
        this(troubleshootingDAO, customerDAO, new CustomerAssignmentDAO());
    }

    TroubleshootingServlet(
            TroubleshootingDAO troubleshootingDAO,
            CustomerDAO customerDAO,
            CustomerAssignmentDAO customerAssignmentDAO) {
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
        this.customerDAO = Objects.requireNonNull(
                customerDAO, "customerDAO");
        this.customerAssignmentDAO = Objects.requireNonNull(
                customerAssignmentDAO, "customerAssignmentDAO");
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 세션 확인
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.expose(request, session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        FlashMessage.expose(request);

        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "list";
        }

        if ("list".equals(viewType)) {
            // 목록/검색 조회
            String normalizedQuery;
            try {
                normalizedQuery = SearchQueryPolicy.normalize(
                        request.getParameter("q"));
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }
            boolean includeContent = "content".equals(
                    request.getParameter("scope"));
            PageResult<TroubleshootingDTO> page =
                    troubleshootingDAO.getTroubleshootingPage(
                            normalizedQuery,
                            includeContent,
                            requestMapper.requestedPage(request),
                            requestMapper.requestedPageSize(request));
            if (normalizedQuery != null) {
                request.setAttribute("q", normalizedQuery);
            }
            request.setAttribute(
                    "searchScope",
                    includeContent ? "content" : "summary");

            request.setAttribute("troubleshootingList", page.items());
            request.setAttribute(
                    "canCreateTroubleshooting",
                    !assignedCustomerNames(user).isEmpty());
            request.setAttribute("currentPage", page.page());
            request.setAttribute("pageSize", page.pageSize());
            request.setAttribute("totalPages", page.totalPages());
            request.setAttribute("totalCount", page.totalCount());
            request.setAttribute("viewType", "list");
            request.getRequestDispatcher("/troubleshooting/troubleshooting_list.jsp").forward(request, response);

        } else if ("add".equals(viewType)) {
            // 등록 폼
            List<CustomerDTO> customerList = assignedCustomers(user);
            request.setAttribute("customerList", customerList);
            String requestedCustomer = request.getParameter("customerName");
            if (requestedCustomer != null && !requestedCustomer.isBlank()) {
                CustomerDTO customer = customerDAO.getCustomerByName(
                        requestedCustomer.strip());
                if (customer != null) {
                    TroubleshootingDTO draft = new TroubleshootingDTO();
                    draft.setCustomerName(customer.getCustomerName());
                    request.setAttribute("troubleshooting", draft);
                }
            }
            request.setAttribute("viewType", "add");
            request.getRequestDispatcher("/troubleshooting/troubleshooting_add.jsp").forward(request, response);

        } else if ("view".equals(viewType)) {
            // 상세 조회
            int id;
            try {
                id = requestMapper.positiveInt(request, "id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            TroubleshootingDTO troubleshooting =
                    troubleshootingDAO.getTroubleshootingById(id);
            if (troubleshooting != null) {
                request.setAttribute("troubleshooting", troubleshooting);
                request.setAttribute(
                        "canManageTroubleshooting",
                        canManageCustomer(user, troubleshooting.getCustomerName()));
                request.setAttribute("viewType", "view");
                request.getRequestDispatcher(
                        "/troubleshooting/troubleshooting_view.jsp").forward(request, response);
            } else {
                FlashMessage.redirect(
                        request,
                        response,
                        "troubleshooting?view=list",
                        "해당 트러블 슈팅 정보를 찾을 수 없습니다.",
                        "error");
            }

        } else if ("edit".equals(viewType)) {
            // 수정 폼
            int id;
            try {
                id = requestMapper.positiveInt(request, "id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            TroubleshootingDTO troubleshooting =
                    troubleshootingDAO.getTroubleshootingById(id);
            if (troubleshooting != null) {
                if (canManageCustomer(user, troubleshooting.getCustomerName())) {
                    List<CustomerDTO> customerList = assignedCustomers(user);

                    request.setAttribute("troubleshooting", troubleshooting);
                    request.setAttribute("customerList", customerList);
                    request.setAttribute("viewType", "edit");
                    request.getRequestDispatcher(
                            "/troubleshooting/troubleshooting_edit.jsp")
                            .forward(request, response);
                } else {
                    FlashMessage.redirect(
                            request,
                            response,
                            "troubleshooting?view=view&id=" + id,
                            "수정 권한이 없습니다.",
                            "error");
                }
            } else {
                FlashMessage.redirect(
                        request,
                        response,
                        "troubleshooting?view=list",
                        "해당 트러블 슈팅 정보를 찾을 수 없습니다.",
                        "error");
            }

        } else {
            response.sendRedirect("troubleshooting?view=list");
        }
    }

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserDTO user = SessionPrincipal.from(session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String actionType = request.getParameter("action");

        if ("add".equals(actionType)) {
            TroubleshootingDTO troubleshooting;
            try {
                troubleshooting = requestMapper.mapCreate(request, user);
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            if (!canManageCustomer(user, troubleshooting.getCustomerName())) {
                redirectForbidden(request, response, "등록");
                return;
            }

            boolean success =
                    troubleshootingDAO.addTroubleshooting(troubleshooting);
            FlashMessage.redirect(
                    request,
                    response,
                    "troubleshooting?view=list",
                    success
                            ? "트러블 슈팅이 성공적으로 등록되었습니다."
                            : "트러블 슈팅 등록 중 오류가 발생했습니다.",
                    success ? "success" : "error");

        } else if ("update".equals(actionType)) {
            // 트러블 슈팅 수정
            TroubleshootingDTO troubleshooting;
            try {
                troubleshooting = requestMapper.mapUpdate(request);
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            int id = troubleshooting.getId();
            TroubleshootingDTO existing =
                    troubleshootingDAO.getTroubleshootingById(id);
            if (existing == null
                    || !canManageCustomer(
                            user, existing.getCustomerName())
                    || !canManageCustomer(
                            user, troubleshooting.getCustomerName())) {
                redirectForbidden(request, response, "수정");
                return;
            }
            boolean success =
                    troubleshootingDAO.updateTroubleshootingForCustomer(
                            troubleshooting, existing.getCustomerName());
            if (success) {
                FlashMessage.redirect(
                        request,
                        response,
                        "troubleshooting?view=view&id=" + id,
                        "트러블 슈팅이 성공적으로 수정되었습니다.",
                        "success");
            } else {
                FlashMessage.redirect(
                        request,
                        response,
                        "troubleshooting?view=list",
                        "수정 권한이 없거나 트러블 슈팅 정보를 찾을 수 없습니다.",
                        "error");
            }

        } else if ("delete".equals(actionType)) {
            // 트러블 슈팅 삭제
            int id;
            try {
                id = requestMapper.positiveInt(request, "id");
            } catch (IllegalArgumentException exception) {
                sendBadRequest(request, response, exception);
                return;
            }

            TroubleshootingDTO existing =
                    troubleshootingDAO.getTroubleshootingById(id);
            if (existing == null
                    || !canManageCustomer(
                            user, existing.getCustomerName())) {
                redirectForbidden(request, response, "삭제");
                return;
            }
            boolean success =
                    troubleshootingDAO.deleteTroubleshootingForCustomer(
                            id, existing.getCustomerName());
            FlashMessage.redirect(
                    request,
                    response,
                    "troubleshooting?view=list",
                    success
                            ? "트러블 슈팅이 성공적으로 삭제되었습니다."
                            : "삭제 권한이 없거나 트러블 슈팅 정보를 찾을 수 없습니다.",
                    success ? "success" : "error");

        } else {
            response.sendRedirect("troubleshooting?view=list");
        }
    }

    private List<CustomerDTO> assignedCustomers(UserDTO user) {
        Set<String> customerNames = assignedCustomerNames(user);
        return customerDAO.getAllCustomers("", "ASC").stream()
                .filter(customer -> customerNames.contains(
                        customer.getCustomerName()))
                .toList();
    }

    private Set<String> assignedCustomerNames(UserDTO user) {
        return customerAssignmentDAO.getCustomerNamesByAssignee(
                user.getUserId(), user.getUserName());
    }

    private boolean canManageCustomer(UserDTO user, String customerName) {
        return customerName != null
                && assignedCustomerNames(user).contains(customerName);
    }

    private static void redirectForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            String action) throws IOException {
        FlashMessage.redirect(
                request,
                response,
                "troubleshooting?view=list",
                "담당 고객사의 트러블 슈팅만 " + action + "할 수 있습니다.",
                "error");
    }

    private static void sendBadRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            IllegalArgumentException exception) throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "invalid_troubleshooting_request",
                exception.getMessage());
    }
}
