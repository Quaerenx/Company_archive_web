package com.company.controller;

import com.company.filerepo.FileRepositoryConfig;
import com.company.filerepo.FileRepositoryException;
import com.company.filerepo.FileRepositoryJson;
import com.company.filerepo.FileRepositoryService;
import com.company.filerepo.FileRepositoryService.DownloadFile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRepositoryDownloadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryDownloadServlet.class);

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            DownloadFile file = service.openDownload(request.getParameter("path"), request.getParameter("id"));
            String encodedName = URLEncoder.encode(file.originalName(), StandardCharsets.UTF_8).replace("+", "%20");
            String fallbackName = file.originalName().replaceAll("[^A-Za-z0-9._-]", "_");
            if (fallbackName.isBlank()) {
                fallbackName = "download";
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/octet-stream");
            response.setContentLengthLong(file.size());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encodedName);
            response.setHeader("Cache-Control", "private, no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            try (InputStream input = Files.newInputStream(file.path())) {
                input.transferTo(response.getOutputStream());
            }
        } catch (FileRepositoryException e) {
            FileRepositoryJson.sendError(response, e.getHttpStatus(), e.getCode(), e.getMessage());
        } catch (IOException e) {
            logger.error("File repository download failed", e);
            if (response.isCommitted()) {
                throw e;
            }
            response.reset();
            FileRepositoryJson.sendError(response, 500, "download_failed", "Unable to stream requested file");
        }
    }
}
