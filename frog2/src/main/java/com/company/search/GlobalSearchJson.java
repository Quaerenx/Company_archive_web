package com.company.search;

import com.company.web.JsonResponse;
import java.util.List;

public final class GlobalSearchJson {
    private GlobalSearchJson() {
    }

    public static String encode(
            String query,
            String contextPath,
            GlobalSearchOutcome outcome) {
        String context = normalizeContextPath(contextPath);
        StringBuilder json = new StringBuilder("{\"query\":")
                .append(JsonResponse.string(query))
                .append(",\"partial\":")
                .append(outcome.partial())
                .append(",\"unavailableCategories\":[");
        for (int index = 0;
                index < outcome.unavailableCategories().size();
                index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(JsonResponse.string(
                    outcome.unavailableCategories().get(index)));
        }
        json.append("]")
                .append(",\"results\":[");
        List<GlobalSearchResult> results = outcome.results();
        for (int index = 0; index < results.size(); index++) {
            GlobalSearchResult result = results.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"category\":")
                    .append(JsonResponse.string(result.category()))
                    .append(",\"label\":")
                    .append(JsonResponse.string(result.label()))
                    .append(",\"description\":")
                    .append(JsonResponse.string(result.description()))
                    .append(",\"url\":")
                    .append(JsonResponse.string(context + result.path()))
                    .append('}');
        }
        return json.append("]}").toString();
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank()
                || "/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/") || contextPath.endsWith("/")
                || contextPath.startsWith("//")) {
            throw new IllegalArgumentException("Invalid servlet context path");
        }
        return contextPath;
    }
}
