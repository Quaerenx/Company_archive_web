package com.company.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class StrictDateParser {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")
                    .withResolverStyle(ResolverStyle.STRICT);

    private StrictDateParser() {
    }

    public static java.util.Date parseDate(String value) throws ParseException {
        LocalDate date = parseLocalDate(value);
        if (date == null) {
            return null;
        }
        return java.util.Date.from(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static java.util.Date parseDateOrNull(String value) {
        try {
            return parseDate(value);
        } catch (ParseException exception) {
            return null;
        }
    }

    public static java.sql.Date parseSqlDateOrNull(String value) {
        try {
            LocalDate date = parseLocalDate(value);
            return date == null ? null : java.sql.Date.valueOf(date);
        } catch (ParseException exception) {
            return null;
        }
    }

    public static Timestamp parseTimestampOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Timestamp.valueOf(
                    LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public static String formatDate(java.util.Date value) {
        if (value == null) {
            return "";
        }
        LocalDate date = value instanceof java.sql.Date sqlDate
                ? sqlDate.toLocalDate()
                : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    private static LocalDate parseLocalDate(String value) throws ParseException {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException exception) {
            ParseException parseException =
                    new ParseException("Invalid date", exception.getErrorIndex());
            parseException.initCause(exception);
            throw parseException;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
