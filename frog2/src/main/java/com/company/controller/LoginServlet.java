package com.company.controller;

import com.company.model.DataAccessException;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.LoginAttemptLimiter;
import com.company.security.SessionPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginServlet extends HttpServlet {
    @FunctionalInterface
    interface Authenticator {
        UserDTO authenticate(String userId, String password);
    }

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);
    private final Authenticator authenticator;
    private final LoginAttemptLimiter attemptLimiter;

    public LoginServlet() {
        this(new UserDAO()::authenticateUser, new LoginAttemptLimiter());
    }

    LoginServlet(Authenticator authenticator) {
        this(authenticator, new LoginAttemptLimiter());
    }

    LoginServlet(
            Authenticator authenticator,
            LoginAttemptLimiter attemptLimiter) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.attemptLimiter = Objects.requireNonNull(
                attemptLimiter, "attemptLimiter");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SessionPrincipal.from(request) != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String password = request.getParameter("password");
        String clientAddress = request.getRemoteAddr();

        LoginAttemptLimiter.Decision initialDecision =
                attemptLimiter.check(clientAddress, userId);
        if (!initialDecision.allowed()) {
            sendRateLimited(request, response, initialDecision);
            return;
        }

        if (userId == null || userId.isBlank()
                || password == null || password.isEmpty()) {
            LoginAttemptLimiter.Decision failedDecision =
                    attemptLimiter.recordFailure(clientAddress, userId);
            if (!failedDecision.allowed()) {
                sendRateLimited(request, response, failedDecision);
            } else {
                sendInvalidCredentials(request, response);
            }
            return;
        }

        try {
            UserDTO user = authenticator.authenticate(userId, password);
            if (user == null) {
                LoginAttemptLimiter.Decision failedDecision =
                        attemptLimiter.recordFailure(clientAddress, userId);
                if (!failedDecision.allowed()) {
                    sendRateLimited(request, response, failedDecision);
                } else {
                    sendInvalidCredentials(request, response);
                }
                return;
            }

            attemptLimiter.recordSuccess(userId);
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                try {
                    oldSession.invalidate();
                } catch (IllegalStateException ignored) {
                    // The session was already invalidated by another request.
                }
            }
            HttpSession session = request.getSession(true);
            SessionPrincipal.store(session, user);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (DataAccessException exception) {
            logger.error("Login data access failure", exception);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute(
                    "errorMessage",
                    "데이터 서비스 연결 문제로 로그인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private static void sendInvalidCredentials(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        request.setAttribute(
                "errorMessage",
                "아이디 또는 비밀번호가 올바르지 않습니다.");
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    private static void sendRateLimited(
            HttpServletRequest request,
            HttpServletResponse response,
            LoginAttemptLimiter.Decision decision)
            throws ServletException, IOException {
        response.setStatus(429);
        response.setHeader(
                "Retry-After",
                Long.toString(decision.retryAfterSeconds()));
        request.setAttribute(
                "errorMessage",
                "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}
