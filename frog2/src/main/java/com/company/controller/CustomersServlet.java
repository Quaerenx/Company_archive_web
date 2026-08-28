package com.company.controller;

import com.company.model.UserDTO;
import com.company.security.SessionPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;

public class CustomersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final CustomerQueryController queryController;
    private final CustomerCommandController commandController;

    public CustomersServlet() {
        this(new CustomerQueryController(), new CustomerCommandController());
    }

    CustomersServlet(
            CustomerQueryController queryController,
            CustomerCommandController commandController) {
        this.queryController = Objects.requireNonNull(queryController, "queryController");
        this.commandController = Objects.requireNonNull(commandController, "commandController");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        FlashMessage.expose(request);
        queryController.handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (SessionPrincipal.from(session) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        commandController.handle(request, response, session);
    }
}
