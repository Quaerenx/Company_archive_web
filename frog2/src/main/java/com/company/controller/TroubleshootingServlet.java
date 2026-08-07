package com.company.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
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

    public TroubleshootingServlet() {
        this(new TroubleshootingDAO());
    }

    TroubleshootingServlet(TroubleshootingDAO troubleshootingDAO) {
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
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

        String viewType = request.getParameter("view");
        if (viewType == null || viewType.isEmpty()) {
            viewType = "list";
        }

        if ("list".equals(viewType)) {
            // 목록/검색 조회
            String q = request.getParameter("q");
            String normalizedQuery =
                    q == null || q.trim().isEmpty() ? null : q.trim();
            PageResult<TroubleshootingDTO> page =
                    troubleshootingDAO.getTroubleshootingPage(
                            normalizedQuery,
                            requestMapper.requestedPage(request),
                            requestMapper.requestedPageSize(request));
            if (normalizedQuery != null) {
                request.setAttribute("q", normalizedQuery);
            }

            request.setAttribute("troubleshootingList", page.items());
            request.setAttribute("currentPage", page.page());
            request.setAttribute("pageSize", page.pageSize());
            request.setAttribute("totalPages", page.totalPages());
            request.setAttribute("totalCount", page.totalCount());
            request.setAttribute("viewType", "list");
            request.getRequestDispatcher("/troubleshooting/troubleshooting_list.jsp").forward(request, response);

        } else if ("add".equals(viewType)) {
            // 등록 폼
            CustomerDAO customerDAO = new CustomerDAO();
            List<CustomerDTO> customerList = customerDAO.getAllCustomers("", "ASC");
            request.setAttribute("customerList", customerList);
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
                        isOwner(troubleshooting, user));
                request.setAttribute("viewType", "view");
                request.getRequestDispatcher(
                        "/troubleshooting/troubleshooting_view.jsp").forward(request, response);
            } else {
                session.setAttribute("error", "해당 트러블 슈팅 정보를 찾을 수 없습니다.");
                response.sendRedirect("troubleshooting?view=list");
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
                if (isOwner(troubleshooting, user)) {
                    CustomerDAO customerDAO = new CustomerDAO();
                    List<CustomerDTO> customerList =
                            customerDAO.getAllCustomers("", "ASC");

                    request.setAttribute("troubleshooting", troubleshooting);
                    request.setAttribute("customerList", customerList);
                    request.setAttribute("viewType", "edit");
                    request.getRequestDispatcher(
                            "/troubleshooting/troubleshooting_edit.jsp")
                            .forward(request, response);
                } else {
                    session.setAttribute("error", "수정 권한이 없습니다.");
                    response.sendRedirect(
                            "troubleshooting?view=view&id=" + id);
                }
            } else {
                session.setAttribute("error", "해당 트러블 슈팅 정보를 찾을 수 없습니다.");
                response.sendRedirect("troubleshooting?view=list");
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

            boolean success =
                    troubleshootingDAO.addTroubleshooting(troubleshooting);
            if (success) {
                session.setAttribute("message", "트러블 슈팅이 성공적으로 등록되었습니다.");
            } else {
                session.setAttribute("error", "트러블 슈팅 등록 중 오류가 발생했습니다.");
            }
            response.sendRedirect("troubleshooting?view=list");

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
            boolean success =
                    troubleshootingDAO.updateTroubleshootingForOwner(
                            troubleshooting, user.getUserId());
            if (success) {
                session.setAttribute("message", "트러블 슈팅이 성공적으로 수정되었습니다.");
                response.sendRedirect("troubleshooting?view=view&id=" + id);
            } else {
                session.setAttribute(
                        "error",
                        "수정 권한이 없거나 트러블 슈팅 정보를 찾을 수 없습니다.");
                response.sendRedirect("troubleshooting?view=list");
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

            boolean success =
                    troubleshootingDAO.deleteTroubleshootingForOwner(
                            id, user.getUserId());
            if (success) {
                session.setAttribute("message", "트러블 슈팅이 성공적으로 삭제되었습니다.");
            } else {
                session.setAttribute(
                        "error",
                        "삭제 권한이 없거나 트러블 슈팅 정보를 찾을 수 없습니다.");
            }
            response.sendRedirect("troubleshooting?view=list");

        } else {
            response.sendRedirect("troubleshooting?view=list");
        }
    }

    private static boolean isOwner(
            TroubleshootingDTO troubleshooting, UserDTO user) {
        return troubleshooting != null
                && user != null
                && Objects.equals(
                        troubleshooting.getCreatorUserId(),
                        user.getUserId());
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
