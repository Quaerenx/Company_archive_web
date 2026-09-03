package com.company.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Writes a small, explicitly selected table as an Excel-friendly UTF-8 CSV. */
public final class CsvResponse {
    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "[a-z0-9][a-z0-9._-]*\\.csv");

    private CsvResponse() {
    }

    public static void write(
            HttpServletResponse response,
            String filename,
            List<String> headers,
            List<? extends List<?>> rows) throws IOException {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new IllegalArgumentException("CSV filename is invalid");
        }
        for (List<?> row : rows) {
            if (row == null || row.size() != headers.size()) {
                throw new IllegalArgumentException(
                        "CSV rows must match the header width");
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader(
                "Content-Disposition", "attachment; filename=\"" + filename + "\"");
        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writeRow(writer, headers);
        for (List<?> row : rows) {
            writeRow(writer, row);
        }
    }

    private static void writeRow(PrintWriter writer, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(encode(values.get(index)));
        }
        writer.write("\r\n");
    }

    static String encode(Object rawValue) {
        String value = rawValue == null ? "" : rawValue.toString();
        String leadingTrimmed = value.stripLeading();
        if (!leadingTrimmed.isEmpty()
                && "=+-@".indexOf(leadingTrimmed.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
