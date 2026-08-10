package com.company.e2e;

import java.util.List;
import java.util.Locale;

final class SessionCookiePolicy {
    private SessionCookiePolicy() {
    }

    static Inspection inspect(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            String[] segments = header.split(";");
            String cookie = segments[0].trim();
            int separator = cookie.indexOf('=');
            if (separator < 0
                    || !"jsessionid".equals(
                            cookie.substring(0, separator)
                                    .trim()
                                    .toLowerCase(Locale.ROOT))
                    || cookie.substring(separator + 1).isBlank()) {
                continue;
            }

            boolean httpOnly = false;
            boolean secure = false;
            boolean sameSiteStrict = false;
            for (int index = 1; index < segments.length; index++) {
                String attribute = segments[index]
                        .trim()
                        .toLowerCase(Locale.ROOT);
                httpOnly |= "httponly".equals(attribute);
                secure |= "secure".equals(attribute);
                sameSiteStrict |= "samesite=strict".equals(attribute);
            }
            return new Inspection(true, httpOnly, secure, sameSiteStrict);
        }
        return new Inspection(false, false, false, false);
    }

    record Inspection(
            boolean sessionCookiePresent,
            boolean httpOnly,
            boolean secure,
            boolean sameSiteStrict) {
    }
}
