package com.company.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// @WebServlet("/logout") - web.xml에서 매핑하므로 주석 처리
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sessionCookieName = resolveSessionCookieName(request);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        expireSessionCookie(request, response, sessionCookieName);
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private static String resolveSessionCookieName(HttpServletRequest request) {
        String requestedSessionId = request.getRequestedSessionId();
        Cookie[] cookies = request.getCookies();
        if (requestedSessionId != null && cookies != null) {
            for (Cookie cookie : cookies) {
                if (requestedSessionId.equals(cookie.getValue())) {
                    return cookie.getName();
                }
            }
        }
        return "JSESSIONID";
    }

    private static void expireSessionCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String sessionCookieName) {
        Cookie cookie = new Cookie(sessionCookieName, "");
        String contextPath = request.getContextPath();
        cookie.setPath(contextPath == null || contextPath.isEmpty()
                ? "/"
                : contextPath);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
