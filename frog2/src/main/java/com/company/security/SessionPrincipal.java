package com.company.security;

import com.company.model.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;

public final class SessionPrincipal {
    public static final String SESSION_ATTRIBUTE = "user";
    public static final String REQUEST_ATTRIBUTE = "user";

    private SessionPrincipal() {
    }

    public static UserDTO from(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value instanceof UserDTO user ? user : null;
    }

    public static UserDTO from(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object requestValue = request.getAttribute(REQUEST_ATTRIBUTE);
        if (requestValue instanceof UserDTO user) {
            return user;
        }
        return from(request.getSession(false));
    }

    public static UserDTO expose(HttpServletRequest request) {
        Objects.requireNonNull(request, "request");
        return expose(request, request.getSession(false));
    }

    public static UserDTO expose(HttpServletRequest request, HttpSession session) {
        Objects.requireNonNull(request, "request");
        Object sessionValue = session == null
                ? null
                : session.getAttribute(SESSION_ATTRIBUTE);
        UserDTO user = sessionValue instanceof UserDTO typedUser ? typedUser : null;
        if (session != null && sessionValue != null && user == null) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
        if (user == null) {
            request.removeAttribute(REQUEST_ATTRIBUTE);
        } else {
            request.setAttribute(REQUEST_ATTRIBUTE, user);
        }
        return user;
    }

    public static void store(HttpSession session, UserDTO user) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(user, "user");
        session.setAttribute(SESSION_ATTRIBUTE, sanitizedCopy(user));
    }

    public static void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
    }

    public static void invalidate(HttpServletRequest request) {
        request.removeAttribute(REQUEST_ATTRIBUTE);
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // Another request may have already invalidated the session.
            }
        }
    }

    private static UserDTO sanitizedCopy(UserDTO user) {
        UserDTO copy = new UserDTO(
                user.getUserId(),
                "",
                user.getUserName(),
                user.getDepartment());
        copy.setAuthenticationFingerprint(user.getAuthenticationFingerprint());
        return copy;
    }
}
