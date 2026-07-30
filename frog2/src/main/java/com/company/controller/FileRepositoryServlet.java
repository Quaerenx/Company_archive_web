package com.company.controller;

import com.company.filerepo.FileRepositoryConfig;
import com.company.filerepo.FileRepositoryException;
import com.company.filerepo.FileRepositoryListing;
import com.company.filerepo.FileRepositoryService;
import com.company.web.ApplicationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FileRepositoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private FileRepositoryService service;

    @Override
    public void init() throws ServletException {
        try {
            service = new FileRepositoryService(FileRepositoryConfig.repositoryRoot());
        } catch (IOException | IllegalStateException e) {
            throw new ServletException("File repository is not configured safely", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            FileRepositoryListing listing = service.list(request.getParameter("path"));
            request.setAttribute("listing", listing);
            request.getRequestDispatcher("/WEB-INF/views/filerepo/list.jsp").forward(request, response);
        } catch (FileRepositoryException e) {
            ApplicationError.send(
                    request, response, e.getHttpStatus(), e.getCode(), e.getMessage());
        }
    }
}
