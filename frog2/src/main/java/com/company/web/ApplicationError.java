package com.company.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ApplicationError {
    private ApplicationError() {
    }

    public static void send(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setHeader("Cache-Control", "no-store");
        if (JsonResponse.isExpected(request)) {
            JsonResponse.sendError(response, status, code, message);
        } else {
            response.sendError(status);
        }
    }
}
