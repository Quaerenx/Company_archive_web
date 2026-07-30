package com.company.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Set;

final class FlashMessage {
    private static final String SESSION_KEY = FlashMessage.class.getName() + ".message";
    private static final Set<String> TYPES = Set.of("success", "error", "warning", "info");

    private FlashMessage() {
    }

    static void store(HttpServletRequest request, String text, String type) {
        if (text == null || text.isBlank()) {
            return;
        }
        String safeType = TYPES.contains(type) ? type : "info";
        request.getSession(true).setAttribute(SESSION_KEY, new Value(text, safeType));
    }

    static void expose(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object stored = session.getAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_KEY);
        if (stored instanceof Value value) {
            request.setAttribute("message", value.text());
            request.setAttribute("messageType", value.type());
        }
    }

    private record Value(String text, String type) {
    }
}
