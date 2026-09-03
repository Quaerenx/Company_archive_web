package com.company.controller;

import com.company.model.UserDTO;
import com.company.mypage.WorkInbox;
import com.company.security.SessionPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public final class WorkInboxServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MyPageQueryService queryService;

    public WorkInboxServlet() {
        this(new MyPageQueryService());
    }

    WorkInboxServlet(MyPageQueryService queryService) {
        this.queryService = Objects.requireNonNull(
                queryService, "queryService");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        WorkInbox inbox = queryService.loadWorkInbox(user, Integer.MAX_VALUE);
        request.setAttribute("workInbox", inbox);
        request.setAttribute("workInboxUserId", user.getUserId());
        request.getRequestDispatcher("/mypage/work_inbox_list.jsp")
                .forward(request, response);
    }
}
