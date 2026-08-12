package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DesignAssetConsolidationTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path CSS = WEBAPP.resolve("resources/css");
    private static final Pattern PAGE_CSS = Pattern.compile(
            "<c:set\\b(?=[^>]*\\bvar=\"pageCss\")"
                    + "(?=[^>]*\\bvalue=\"([^\"]+)\")[^>]*>");
    private static final Pattern PAGE_BODY_FONT_OVERRIDE = Pattern.compile(
            "\\bbody(?:\\.[\\w-]+)*\\s*\\{[^}]*\\bfont-family\\s*:",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STYLESHEET_LINK = Pattern.compile(
            "<link\\b[^>]*\\brel\\s*=\\s*['\"]stylesheet['\"]",
            Pattern.CASE_INSENSITIVE);

    @Test
    void coreStylesHaveOneOrderedDefinitionSource() throws Exception {
        Path fragmentPath = WEBAPP.resolve("WEB-INF/includes/core_styles.jspf");
        assertTrue(Files.isRegularFile(fragmentPath), fragmentPath.toString());

        String fragment = Files.readString(fragmentPath);
        String tokens = "/resources/css/tokens.css";
        String base = "/resources/css/base.css";
        String components = "/resources/css/components.css";
        String uiSystem = "/resources/css/ui-system.css";
        String utilities = "/resources/css/utilities.css";

        assertEquals(1, countOccurrences(fragment, tokens));
        assertEquals(1, countOccurrences(fragment, base));
        assertEquals(1, countOccurrences(fragment, components));
        assertEquals(1, countOccurrences(fragment, uiSystem));
        assertEquals(1, countOccurrences(fragment, utilities));
        assertFalse(fragment.contains("/resources/css/main_style.css"));
        assertTrue(fragment.indexOf(tokens) < fragment.indexOf(base));
        assertTrue(fragment.indexOf(base) < fragment.indexOf(components));
        assertTrue(fragment.indexOf(components) < fragment.indexOf(uiSystem));
        assertTrue(fragment.indexOf(uiSystem) < fragment.indexOf(utilities));

        String header = readWebapp("includes/header.jsp");
        assertTrue(header.contains("include file=\"/WEB-INF/includes/core_styles.jspf\""));
        assertFalse(header.contains(tokens));
        assertFalse(header.contains(base));
        assertFalse(header.contains(components));
        assertFalse(header.contains(uiSystem));
        assertFalse(header.contains(utilities));
    }

    @Test
    void headerLoadsOrderedPageStylesAfterHeaderCss() throws Exception {
        String header = readWebapp("includes/header.jsp");
        int coreStyles = header.indexOf("include file=\"/WEB-INF/includes/core_styles.jspf\"");
        int vendorCss = header.indexOf(
                "/resources/vendor/fontawesome-free/5.15.4/css/all.min.css");
        int headerCss = header.indexOf("/resources/css/pages/header.css");
        int pageCss = header.indexOf("not empty pageCss");
        int headEnd = header.indexOf("</head>");

        assertTrue(coreStyles >= 0, "core style include is missing");
        assertTrue(vendorCss > coreStyles, "vendor styles must follow core styles");
        assertTrue(headerCss > vendorCss, "header.css must follow vendor styles");
        assertTrue(pageCss > headerCss, "pageCss must follow header.css");
        assertTrue(headEnd > pageCss, "pageCss must remain inside <head>");
        assertFalse(header.contains("pageCssBeforeVendor"));
        assertFalse(header.contains("pageCssAfterHeader"));

        String orderedSlot = header.substring(pageCss, headEnd);
        assertTrue(orderedSlot.contains("<c:forTokens"));
        assertTrue(orderedSlot.contains("items=\"${pageCss}\""));
        assertTrue(orderedSlot.contains("delims=\",\""));
    }

    @Test
    void pageStylesDoNotOverrideSharedBodyTypeface() throws Exception {
        Path pages = CSS.resolve("pages");
        try (var paths = Files.walk(pages)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".css"))
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(
                        PAGE_BODY_FONT_OVERRIDE.matcher(source).find(),
                        () -> "page stylesheet overrides shared body typeface: "
                                + pages.relativize(path));
            }
        }
    }

    @Test
    void sharedStyleLayersDoNotRedeclareTheSameSelectors() throws Exception {
        Map<String, String> selectorOwner = new LinkedHashMap<>();
        for (String stylesheet : new String[] {
                "base.css",
                "components.css",
                "ui-system.css",
                "utilities.css",
                "pages/header.css"
        }) {
            for (String selector : selectors(
                    Files.readString(CSS.resolve(stylesheet)))) {
                String existingOwner =
                        selectorOwner.putIfAbsent(selector, stylesheet);
                assertTrue(existingOwner == null,
                        () -> selector + " is declared in both "
                                + existingOwner + " and " + stylesheet);
            }
        }
    }

    @Test
    void customerDomainStylesAreNotLoadedByOtherDomains() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".jsp"))
                    .toList()) {
                String relative = WEBAPP.relativize(path)
                        .toString()
                        .replace('\\', '/');
                if (!relative.startsWith("customers/")) {
                    assertFalse(Files.readString(path).contains(
                                    "/resources/css/pages/customers.css"),
                            relative);
                }
            }
        }
    }

    @Test
    void pageStylesAreDeclaredBeforeTheSharedHeader() throws Exception {
        Map<String, String> expectedPageStyles = new LinkedHashMap<>();
        expectedPageStyles.put(
                "customers/customers_add.jsp",
                "/resources/css/pages/customers.css");
        expectedPageStyles.put(
                "customers/customers_detail.jsp",
                "/resources/css/pages/customers.css,"
                        + "/resources/css/pages/customer_detail.css");
        expectedPageStyles.put(
                "customers/customers_detail_edit.jsp",
                "/resources/css/pages/customers.css");
        expectedPageStyles.put(
                "customers/customers_edit.jsp",
                "/resources/css/pages/customers.css");
        expectedPageStyles.put(
                "maintenance/maintenance_add.jsp",
                "/resources/css/pages/maintenance.css");
        expectedPageStyles.put(
                "maintenance/maintenance_cards.jsp",
                "/resources/css/pages/maintenance_cards.css");
        expectedPageStyles.put(
                "maintenance/maintenance_edit.jsp",
                "/resources/css/pages/maintenance.css");
        expectedPageStyles.put(
                "maintenance/maintenance_history.jsp",
                "/resources/css/pages/maintenance_history.css");
        expectedPageStyles.put(
                "meeting/meeting_edit.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_form.css");
        expectedPageStyles.put(
                "meeting/meeting_list.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_list_layout.css,"
                        + "/resources/css/pages/meeting_list.css");
        expectedPageStyles.put(
                "meeting/meeting_view.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_view.css");
        expectedPageStyles.put(
                "meeting/meeting_write.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_form.css");

        for (Map.Entry<String, String> entry : expectedPageStyles.entrySet()) {
            String page = readWebapp(entry.getKey());
            Matcher declaration = PAGE_CSS.matcher(page);
            assertTrue(declaration.find(),
                    () -> "pageCss is missing: " + entry.getKey());
            assertEquals(entry.getValue(), normalizeCommaList(declaration.group(1)),
                    entry.getKey());

            int headerInclude = page.indexOf("include file=\"/includes/header.jsp\"");
            assertTrue(headerInclude >= 0, entry.getKey());
            assertFalse(STYLESHEET_LINK.matcher(page.substring(headerInclude)).find(),
                    () -> "stylesheet link remains after header include: " + entry.getKey());
        }
    }

    @Test
    void customerFormsUseOneSharedStyleScope() throws Exception {
        String add = readWebapp("customers/customers_add.jsp");
        String edit = readWebapp("customers/customers_edit.jsp");
        String customers = Files.readString(CSS.resolve("pages/customers.css"));

        assertTrue(hasClass(add, "customer-form-page"));
        assertTrue(hasClass(edit, "customer-form-page"));
        assertTrue(hasClass(add, "content-management"));
        assertTrue(hasClass(edit, "content-management"));
        assertTrue(Files.readString(CSS.resolve("components.css"))
                .contains(".content-management {"));
        assertFalse(Pattern.compile(
                        "(?m)^\\.customer-management\\s*\\{")
                .matcher(customers)
                .find());
        assertTrue(customers.contains(".customer-form-page > .container"));
        assertTrue(add.contains("ui-form-card"));
        assertTrue(edit.contains("ui-form-card"));
        assertTrue(add.contains("ui-form ui-form-layout"));
        assertTrue(edit.contains("ui-form ui-form-layout"));
        assertFalse(customers.contains(
                ".customer-form-page .form-container .form-group"));
        assertFalse(customers.contains(".customer-add-page > .container"));
        assertFalse(customers.contains(".customer-edit-page > .container"));
    }

    @Test
    void accountFormsUseSharedComponentRules() throws Exception {
        String password = readWebapp("mypage/change_password.jsp");
        String profile = readWebapp("mypage/edit_profile.jsp");
        String components = Files.readString(CSS.resolve("components.css"));
        String passwordCss = Files.readString(CSS.resolve("pages/password_change.css"));
        String profileCss = Files.readString(CSS.resolve("pages/profile_edit.css"));

        assertTrue(hasClass(password, "account-form-container"));
        assertTrue(hasClass(profile, "account-form-container"));
        assertTrue(components.contains(".account-form-container {"));
        assertTrue(components.contains(".account-form-container .form-card"));
        assertTrue(components.contains(".account-form-container .form-control"));
        assertTrue(components.contains(".account-form-container .form-actions"));
        assertTrue(components.contains(".account-form-container .help-text"));
        assertFalse(passwordCss.contains(".password-container .form-card"));
        assertFalse(profileCss.contains(".edit-container .form-card"));
    }

    @Test
    void footerLoadsOrderedPageScriptsFromDeclarations() throws Exception {
        String footer = readWebapp("includes/footer.jsp");
        int vendorSlot = footer.indexOf("not empty vendorScript");
        int pageSlot = footer.indexOf("not empty pageScript");

        assertTrue(vendorSlot >= 0, "vendorScript slot is missing");
        assertTrue(pageSlot > vendorSlot, "vendorScript must precede pageScript");
        String orderedPageSlot = footer.substring(pageSlot);
        assertTrue(orderedPageSlot.contains("<c:forTokens"));
        assertTrue(orderedPageSlot.contains("items=\"${pageScript}\""));
        assertTrue(orderedPageSlot.contains("delims=\",\""));

        Map<String, String> expectedPageScripts = new LinkedHashMap<>();
        expectedPageScripts.put("maintenance/maintenance_add.jsp", "/resources/js/pages/maintenance_form.js");
        expectedPageScripts.put("maintenance/maintenance_cards.jsp", "/resources/js/pages/maintenance_cards.js");
        expectedPageScripts.put("maintenance/maintenance_edit.jsp", "/resources/js/pages/maintenance_form.js");
        expectedPageScripts.put("maintenance/maintenance_history.jsp", "/resources/js/pages/maintenance_history.js");
        expectedPageScripts.put("meeting/meeting_edit.jsp", "/resources/js/pages/meeting_form.js");
        expectedPageScripts.put("meeting/meeting_list.jsp", "/resources/js/pages/meeting_list.js");
        expectedPageScripts.put("meeting/meeting_view.jsp", "/resources/js/pages/meeting_view.js");
        expectedPageScripts.put("meeting/meeting_write.jsp", "/resources/js/pages/meeting_form.js");
        Pattern directPageScript = Pattern.compile(
                "<script\\b[^>]*\\bsrc\\s*=\\s*['\"][^'\"]*/resources/js/pages/[^'\"]+['\"]",
                Pattern.CASE_INSENSITIVE);

        for (Map.Entry<String, String> entry : expectedPageScripts.entrySet()) {
            String page = readWebapp(entry.getKey());
            assertEquals(entry.getValue(), declaredValue(page, "pageScript"), entry.getKey());
            int headerInclude = page.indexOf("include file=\"/includes/header.jsp\"");
            assertTrue(headerInclude >= 0, entry.getKey());
            assertFalse(directPageScript.matcher(page.substring(headerInclude)).find(),
                    () -> "direct page script remains: " + entry.getKey());
        }

        String history = readWebapp("maintenance/maintenance_history.jsp");
        assertEquals("${pageContext.request.contextPath}"
                        + "/resources/vendor/chart.js/4.4.4/chart.umd.min.js"
                        + "?v=${frog2AssetVersion}",
                declaredValue(history, "vendorScript"));
    }

    @Test
    void canonicalFileRepositoryPathActivatesHeaderNavigation() throws Exception {
        String header = readWebapp("WEB-INF/includes/header_nav.jspf");

        assertTrue(header.contains("navFileRepositoryPath"));
        assertTrue(header.contains("/file-repository"));
        assertTrue(header.contains("navLegacyFileRepositoryPath"));
        assertTrue(header.contains("/filerepo"));
        assertTrue(header.contains("navFileRepositoryCurrent"));
    }

    private static String readWebapp(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static int countOccurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }

    private static String declaredValue(String source, String variable) {
        Pattern declaration = Pattern.compile(
                "<c:set\\b(?=[^>]*\\bvar=\"" + Pattern.quote(variable) + "\")"
                        + "(?=[^>]*\\bvalue=\"([^\"]+)\")[^>]*>");
        Matcher matcher = declaration.matcher(source);
        assertTrue(matcher.find(), variable + " declaration is missing");
        return matcher.group(1);
    }

    private static String normalizeCommaList(String value) {
        return value.trim().replaceAll("\\s*,\\s*", ",");
    }

    private static Set<String> selectors(String stylesheet) {
        String withoutComments = stylesheet.replaceAll(
                "(?s)/\\*.*?\\*/", "");
        Set<String> selectors = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("([^{}]+)\\{")
                .matcher(withoutComments);
        while (matcher.find()) {
            String selectorList = matcher.group(1).trim();
            if (selectorList.startsWith("@")
                    || selectorList.matches("(?:from|to|\\d+%)")) {
                continue;
            }
            for (String selector : splitSelectorList(selectorList)) {
                String normalized = selector.trim()
                        .replaceAll("\\s+", " ");
                if (!normalized.isEmpty()) {
                    selectors.add(normalized);
                }
            }
        }
        return selectors;
    }

    private static Set<String> splitSelectorList(String selectorList) {
        Set<String> selectors = new LinkedHashSet<>();
        StringBuilder current = new StringBuilder();
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        char quote = 0;

        for (int index = 0; index < selectorList.length(); index++) {
            char character = selectorList.charAt(index);
            if (quote != 0) {
                current.append(character);
                if (character == quote
                        && (index == 0 || selectorList.charAt(index - 1) != '\\')) {
                    quote = 0;
                }
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
            } else if (character == '(') {
                parenthesisDepth++;
            } else if (character == ')') {
                parenthesisDepth--;
            } else if (character == '[') {
                bracketDepth++;
            } else if (character == ']') {
                bracketDepth--;
            } else if (character == ','
                    && parenthesisDepth == 0
                    && bracketDepth == 0) {
                selectors.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        selectors.add(current.toString());
        return selectors;
    }

    private static boolean hasClass(String source, String className) {
        Pattern classAttribute = Pattern.compile(
                "class\\s*=\\s*['\"][^'\"]*\\b" + Pattern.quote(className)
                        + "\\b[^'\"]*['\"]");
        return classAttribute.matcher(source).find();
    }
}
