package com.company.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class FlashMessage {
    static final String PARAMETER_NAME = "_flash";
    static final int MAX_MESSAGES = 20;
    static final long TTL_MILLIS = 5 * 60 * 1000L;

    private static final String SESSION_KEY = FlashMessage.class.getName() + ".messages";
    private static final Set<String> TYPES = Set.of("success", "error", "warning", "info");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{32}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private FlashMessage() {
    }

    static String store(HttpServletRequest request, String text, String type) {
        return store(request, text, type, System.currentTimeMillis());
    }

    static void expose(HttpServletRequest request) {
        expose(request, System.currentTimeMillis());
    }

    static void redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String location,
            String text,
            String type) throws IOException {
        String token = store(request, text, type);
        response.sendRedirect(appendToken(location, token));
    }

    static String appendToken(String location, String token) {
        if (location == null || token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            return location;
        }
        int fragmentIndex = location.indexOf('#');
        String base = fragmentIndex >= 0 ? location.substring(0, fragmentIndex) : location;
        String fragment = fragmentIndex >= 0 ? location.substring(fragmentIndex) : "";
        String separator;
        if (!base.contains("?")) {
            separator = "?";
        } else if (base.endsWith("?") || base.endsWith("&")) {
            separator = "";
        } else {
            separator = "&";
        }
        return base + separator + PARAMETER_NAME + "=" + token + fragment;
    }

    static String store(
            HttpServletRequest request,
            String text,
            String type,
            long nowMillis) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String safeType = TYPES.contains(type) ? type : "info";
        HttpSession session = request.getSession(true);
        synchronized (session) {
            MessageStore messages = messageStore(session);
            removeExpired(messages, nowMillis);
            while (messages.size() >= MAX_MESSAGES) {
                removeOldest(messages);
            }
            String token;
            do {
                byte[] bytes = new byte[24];
                SECURE_RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            } while (messages.containsKey(token));
            messages.put(token, new Value(text, safeType, nowMillis));
            session.setAttribute(SESSION_KEY, messages);
            return token;
        }
    }

    static void expose(HttpServletRequest request, long nowMillis) {
        String token = request.getParameter(PARAMETER_NAME);
        if (token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Value value;
        synchronized (session) {
            Object stored = session.getAttribute(SESSION_KEY);
            if (!(stored instanceof MessageStore messages)) {
                return;
            }
            removeExpired(messages, nowMillis);
            value = messages.remove(token);
            if (messages.isEmpty()) {
                session.removeAttribute(SESSION_KEY);
            } else {
                session.setAttribute(SESSION_KEY, messages);
            }
        }
        if (value != null) {
            request.setAttribute("message", value.text());
            request.setAttribute("messageType", value.type());
        }
    }

    private static MessageStore messageStore(HttpSession session) {
        Object stored = session.getAttribute(SESSION_KEY);
        return stored instanceof MessageStore messages ? messages : new MessageStore();
    }

    private static void removeExpired(MessageStore messages, long nowMillis) {
        messages.entrySet().removeIf(
                entry -> nowMillis - entry.getValue().createdAtMillis() >= TTL_MILLIS);
    }

    private static void removeOldest(MessageStore messages) {
        Iterator<Map.Entry<String, Value>> iterator = messages.entrySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static final class MessageStore extends LinkedHashMap<String, Value>
            implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    private record Value(String text, String type, long createdAtMillis)
            implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
