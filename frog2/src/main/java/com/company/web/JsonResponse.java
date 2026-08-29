package com.company.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class JsonResponse {
    private static final Set<String> FILE_REPOSITORY_UPLOAD_PATHS = Set.of(
            "/file-repository/upload",
            "/filerepo/filerepo_upload.jsp",
            "/filerepo/filerepo_uploadProcess.jsp");
    private static final Set<String> JSON_PATHS = Set.of(
            "/comment",
            "/file-repository/download",
            "/filerepo/filerepo_download.jsp");

    private JsonResponse() {
    }

    public static boolean isExpected(HttpServletRequest request) {
        String path = RequestPaths.relativePath(request);
        if (FILE_REPOSITORY_UPLOAD_PATHS.contains(path)) {
            return "POST".equalsIgnoreCase(request.getMethod());
        }
        if (JSON_PATHS.contains(path)) {
            return true;
        }
        if ("/customers".equals(path)) {
            String action = request.getParameter("action");
            if ("getDetail".equals(action)
                    || "getCustomersForMaintenance".equals(action)) {
                return true;
            }
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json")) {
            return true;
        }
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    public static void sendError(
            HttpServletResponse response, int status, String code, String message) throws IOException {
        write(response, status, "{\"status\":\"error\",\"success\":false,\"code\":"
                + string(code) + ",\"message\":" + string(message) + "}");
    }

    public static void sendSuccess(
            HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, "{\"status\":\"ok\",\"success\":true,\"message\":"
                + string(message) + "}");
    }

    public static void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(body);
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        result.append(String.format("\\u%04x", (int) ch));
                    } else {
                        result.append(ch);
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * Encodes a JSON string value. A {@code null} Java value keeps the legacy
     * response behavior and is encoded as an empty JSON string.
     */
    public static String string(String value) {
        return "\"" + escape(value) + "\"";
    }

    /** Encodes a nullable JSON string value, using the JSON {@code null} literal. */
    public static String nullableString(String value) {
        return value == null ? "null" : string(value);
    }
}
