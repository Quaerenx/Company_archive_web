package com.company.filerepo;

import com.company.filerepo.FileRepositoryService.StoredFile;
import com.company.filerepo.FileRepositoryService.ImportItem;
import com.company.filerepo.FileRepositoryService.ImportResult;
import com.company.web.JsonResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public final class FileRepositoryJson {
    private FileRepositoryJson() {
    }

    public static void sendError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        JsonResponse.sendError(response, status, code, message);
    }

    public static void sendCreated(HttpServletResponse response, List<StoredFile> storedFiles)
            throws IOException {
        StringBuilder json = new StringBuilder(
                "{\"status\":\"ok\",\"success\":true,\"files\":[");
        for (int index = 0; index < storedFiles.size(); index++) {
            StoredFile file = storedFiles.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(JsonResponse.string(file.id()))
                    .append(",\"name\":")
                    .append(JsonResponse.string(file.originalName()))
                    .append(",\"size\":").append(file.size()).append('}');
        }
        json.append("]}");
        JsonResponse.write(response, HttpServletResponse.SC_CREATED, json.toString());
    }

    public static void sendImportResult(
            HttpServletResponse response,
            ImportResult result,
            String listingUrl) throws IOException {
        StringBuilder json = new StringBuilder(512)
                .append("{\"status\":\"ok\",\"success\":true,\"summary\":{")
                .append("\"imported\":").append(result.importedCount())
                .append(",\"conflicts\":").append(result.conflictCount())
                .append(",\"rejected\":").append(result.rejectedCount())
                .append(",\"deferred\":").append(result.deferredCount())
                .append(",\"failed\":").append(result.failedCount())
                .append("},\"files\":[");
        for (int index = 0; index < result.items().size(); index++) {
            ImportItem item = result.items().get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"path\":").append(JsonResponse.string(item.path()))
                    .append(",\"name\":").append(JsonResponse.string(item.name()))
                    .append(",\"status\":").append(JsonResponse.string(item.status()))
                    .append(",\"label\":").append(JsonResponse.string(item.statusLabel()))
                    .append(",\"reason\":").append(JsonResponse.string(item.reason()))
                    .append(",\"retryable\":")
                    .append("failed".equals(item.status()))
                    .append('}');
        }
        json.append("],\"listingUrl\":")
                .append(JsonResponse.string(listingUrl))
                .append('}');
        JsonResponse.write(response, HttpServletResponse.SC_OK, json.toString());
    }
}
