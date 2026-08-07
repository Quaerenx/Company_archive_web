package com.company.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FaviconServlet extends HttpServlet {
    private static final String VERSION = "20260804-archive-arc-svg-1";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        String target = ctx + "/favicon.svg?v=" + VERSION;
        resp.setStatus(HttpServletResponse.SC_FOUND);
        resp.setHeader("Location", target);
    }
}
