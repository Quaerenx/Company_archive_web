package com.company.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FaviconServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        String version = req.getServletContext().getInitParameter("frog2AssetVersion");
        String target = ctx + "/favicon.svg?v=" + version;
        resp.setStatus(HttpServletResponse.SC_FOUND);
        resp.setHeader("Location", target);
    }
}
