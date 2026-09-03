package com.company.controller;

import com.company.filerepo.FileRepositoryConfig;
import com.company.filerepo.FileRepositoryException;
import com.company.filerepo.FileRepositoryJson;
import com.company.filerepo.FileRepositoryService;
import com.company.filerepo.FileRepositoryService.ImportPreview;
import com.company.filerepo.FileRepositoryService.ImportResult;
import com.company.model.UserDTO;
import com.company.security.AdminAccessPolicy;
import com.company.security.SessionPrincipal;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileRepositoryImportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private FileRepositoryService service;

    public FileRepositoryImportServlet() {
    }

    FileRepositoryImportServlet(FileRepositoryService service) {
        this.service = service;
    }

    @Override
    public void init() throws ServletException {
        if (service != null) {
            return;
        }
        try {
            service = new FileRepositoryService(
                    FileRepositoryConfig.repositoryRoot());
        } catch (IOException | IllegalStateException exception) {
            throw new ServletException(
                    "File repository is not configured safely",
                    exception);
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (!authorize(request, response)) {
            return;
        }
        try {
            ImportPreview preview = service.previewUnmanaged(
                    request.getParameter("path"));
            request.setAttribute("importPreview", preview);
            request.setAttribute(
                    "listingUrl",
                    listingLocation(request, preview.relativePath()));
            request.getRequestDispatcher(
                    "/WEB-INF/views/filerepo/import.jsp")
                    .forward(request, response);
        } catch (FileRepositoryException exception) {
            ApplicationError.send(
                    request,
                    response,
                    exception.getHttpStatus(),
                    exception.getCode(),
                    exception.getMessage());
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (!authorize(request, response)) {
            return;
        }

        try {
            String[] selectedPaths = request.getParameterValues("selectedPath");
            if (selectedPaths == null || selectedPaths.length == 0) {
                throw new FileRepositoryException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "import_selection_required",
                        "At least one server-side file must be selected");
            }
            ImportResult result = service.importUnmanaged(
                    request.getParameter("path"),
                    Arrays.asList(selectedPaths));
            String listingUrl = listingLocation(
                    request, result.relativePath());
            if (JsonResponse.isExpected(request)) {
                FileRepositoryJson.sendImportResult(
                        response, result, listingUrl);
                return;
            }
            String message = "서버 파일 반입 결과: 등록 "
                    + result.importedCount()
                    + "건, 이름 충돌 "
                    + result.conflictCount()
                    + "건, 거부 "
                    + result.rejectedCount()
                    + "건, 안정화 대기 "
                    + result.deferredCount()
                    + "건, 실패 "
                    + result.failedCount()
                    + "건";
            String type = result.failedCount() > 0
                    || result.rejectedCount() > 0
                    || result.conflictCount() > 0
                    || result.deferredCount() > 0
                            ? "warning"
                            : result.importedCount() > 0
                                    ? "success"
                                    : "info";
            FlashMessage.redirect(
                    request,
                    response,
                    listingUrl,
                    message,
                    type);
        } catch (FileRepositoryException exception) {
            ApplicationError.send(
                    request,
                    response,
                    exception.getHttpStatus(),
                    exception.getCode(),
                    exception.getMessage());
        }
    }

    private static boolean authorize(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "Authentication is required");
            return false;
        }
        if (!AdminAccessPolicy.isAdmin(user)) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "admin_access_required",
                    "Administrator access is required");
            return false;
        }
        return true;
    }

    private static String listingLocation(
            HttpServletRequest request, String relativePath) {
        String location = request.getContextPath() + "/file-repository";
        return relativePath == null || relativePath.isEmpty()
                ? location
                : location
                        + "?path="
                        + URLEncoder.encode(
                                relativePath,
                                StandardCharsets.UTF_8);
    }
}
