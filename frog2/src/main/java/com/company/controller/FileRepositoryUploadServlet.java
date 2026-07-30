package com.company.controller;

import com.company.filerepo.FileRepositoryConfig;
import com.company.filerepo.FileRepositoryException;
import com.company.filerepo.FileRepositoryFilePolicy;
import com.company.filerepo.FileRepositoryFilePolicy.ValidatedFile;
import com.company.filerepo.FileRepositoryJson;
import com.company.filerepo.FileRepositoryListing;
import com.company.filerepo.FileRepositoryService;
import com.company.filerepo.FileRepositoryService.StoredFile;
import com.company.web.ApplicationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRepositoryUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryUploadServlet.class);

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
            request.getRequestDispatcher("/WEB-INF/views/filerepo/upload.jsp").forward(request, response);
        } catch (FileRepositoryException e) {
            ApplicationError.send(
                    request, response, e.getHttpStatus(), e.getCode(), e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<StoredFile> storedFiles = new ArrayList<>();
        Collection<Part> requestParts = List.of();
        try {
            requestParts = request.getParts();
            List<Part> fileParts = new ArrayList<>();
            for (Part part : requestParts) {
                String submittedName = part.getSubmittedFileName();
                if (submittedName == null || submittedName.isBlank()) {
                    continue;
                }
                if (!"uploadFiles".equals(part.getName())) {
                    throw new FileRepositoryException(400, "unexpected_file_part", "Unexpected multipart file field");
                }
                fileParts.add(part);
            }
            if (fileParts.isEmpty()) {
                throw new FileRepositoryException(400, "no_files", "No upload files were provided");
            }
            if (fileParts.size() > FileRepositoryFilePolicy.MAX_FILE_COUNT) {
                throw new FileRepositoryException(413, "too_many_files", "At most 5 files may be uploaded");
            }

            List<UploadCandidate> candidates = new ArrayList<>();
            for (Part part : fileParts) {
                ValidatedFile validated = service.validateUpload(
                        part.getSubmittedFileName(), part.getContentType(), part.getSize());
                candidates.add(new UploadCandidate(part, validated));
            }

            String relativePath = request.getParameter("path");
            for (UploadCandidate candidate : candidates) {
                try (InputStream input = candidate.part().getInputStream()) {
                    storedFiles.add(service.store(
                            relativePath,
                            candidate.validated(),
                            candidate.part().getSize(),
                            input));
                }
            }
            FileRepositoryJson.sendCreated(response, storedFiles);
        } catch (FileRepositoryException e) {
            rollback(storedFiles);
            FileRepositoryJson.sendError(response, e.getHttpStatus(), e.getCode(), e.getMessage());
        } catch (IllegalStateException e) {
            rollback(storedFiles);
            FileRepositoryJson.sendError(response, 413, "request_too_large", "Multipart request exceeds configured limits");
        } catch (ServletException e) {
            rollback(storedFiles);
            FileRepositoryJson.sendError(response, 400, "invalid_multipart", "Multipart request is invalid");
        } catch (IOException e) {
            rollback(storedFiles);
            logger.error("File repository upload I/O failure", e);
            if (response.isCommitted()) {
                throw e;
            }
            response.reset();
            FileRepositoryJson.sendError(response, 500, "upload_failed", "Unable to process upload");
        } catch (RuntimeException e) {
            rollback(storedFiles);
            logger.error("Unexpected file repository upload failure", e);
            FileRepositoryJson.sendError(response, 500, "internal_error", "Unable to process upload");
        } finally {
            for (Part part : requestParts) {
                try {
                    part.delete();
                } catch (IOException ignored) {
                    // Container temporary-file cleanup is best effort.
                }
            }
        }
    }

    private void rollback(List<StoredFile> storedFiles) {
        for (StoredFile storedFile : storedFiles) {
            service.rollback(storedFile);
        }
    }

    private record UploadCandidate(Part part, ValidatedFile validated) {
    }
}
