package com.company.filerepo;

import com.company.filerepo.FileRepositoryService.StoredFile;
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
}
