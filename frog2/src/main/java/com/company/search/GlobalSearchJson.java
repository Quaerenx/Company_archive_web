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
                    .append(JsonResponse.string(result.description()));
            boolean hasEnhancements = !result.groupCode().equals(result.category())
                    || result.morePath() != null
                    || !result.actions().isEmpty();
            if (hasEnhancements) {
                json.append(",\"group\":")
                        .append(JsonResponse.string(result.groupCode()));
            }
            json.append(",\"url\":")
                    .append(JsonResponse.string(context + result.path()));
            if (!hasEnhancements) {
                json.append('}');
                continue;
            }
            json.append(",\"moreUrl\":")
                    .append(result.morePath() == null
                            ? "null"
                            : JsonResponse.string(context + result.morePath()))
                    .append(",\"actions\":[");
            for (int actionIndex = 0;
                    actionIndex < result.actions().size();
                    actionIndex++) {
                if (actionIndex > 0) {
                    json.append(',');
                }
                GlobalSearchAction action = result.actions().get(actionIndex);
                json.append("{\"label\":")
                        .append(JsonResponse.string(action.label()))
                        .append(",\"url\":")
                        .append(JsonResponse.string(context + action.path()))
                        .append('}');
            }
            json.append("]}");
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
