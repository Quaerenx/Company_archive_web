package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.filter.AuthFilter;
import com.company.filerepo.FileRepositoryService;
import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.UserDTO;
import com.company.security.AdminAccessPolicy;
import com.company.security.CsrfFilter;
import com.company.security.CsrfToken;
import com.company.util.BusinessDate;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises production filters and servlets with temporary storage and no database. */
class IsolatedAuthenticatedWriteFlowTest {
    @TempDir
    Path temporaryDirectory;

    private Path root;
    private FileRepositoryService repository;

    @BeforeEach
    void initializeRepository() throws Exception {
        root = Files.createDirectory(temporaryDirectory.resolve("repository"));
        repository = new FileRepositoryService(root);
    }

    @Test
    void anonymousAndInvalidCsrfUploadsNeverReadMultipartOrWriteStorage() throws Exception {
        FileRepositoryUploadServlet upload = uploadServlet();
        Request anonymous = new Request("/file-repository/upload", null);
        Response anonymousResponse = filtered(anonymous, upload::doPost);
        assertEquals(401, anonymousResponse.status);
        assertEquals(0, anonymous.partReads);

        Session user = new Session("ordinary-user");
        Session other = new Session("other-user");
        for (String token : List.of("", "invalid-token", other.csrf)) {
            Request request = new Request("/file-repository/upload", user);
            request.headers.put(CsrfToken.HEADER_NAME, token);
            Response response = filtered(request, upload::doPost);
            assertEquals(403, response.status);
            assertTrue(response.body.toString().contains("invalid_csrf"));
            assertEquals(0, request.partReads);
        }
        assertEquals(0, repository.list("").getFileCount());
        try (var files = Files.list(root)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void authenticatedMultipartStoresFilesAndRollsBackOnlyItsFailedBatch() throws Exception {
        FileRepositoryUploadServlet upload = uploadServlet();
        Session user = new Session("ordinary-user");
        UploadPart first = new UploadPart("first.txt", "first content");
        UploadPart second = new UploadPart("second.txt", "second content");
        Request success = authenticated("/file-repository/upload", user);
        success.parts = List.of(first.proxy(), second.proxy());

        assertEquals(201, filtered(success, upload::doPost).status);
        assertEquals(2, repository.list("").getFileCount());
        assertTrue(first.deleted && second.deleted);
        for (var entry : repository.list("").getEntries()) {
            var download = repository.openDownload("", entry.getId());
            assertArrayEquals((entry.getName().equals("first.txt")
                    ? first.content : second.content), Files.readAllBytes(download.path()));
        }

        UploadPart valid = new UploadPart("new-valid.txt", "new content");
        UploadPart active = new UploadPart("disguised.txt", "<html><script>alert(1)</script></html>");
        Request failure = authenticated("/file-repository/upload", user);
        failure.parts = List.of(valid.proxy(), active.proxy());

        Response failed = filtered(failure, upload::doPost);
        assertEquals(415, failed.status);
        assertTrue(valid.deleted && active.deleted);
        assertEquals(Set.of("first.txt", "second.txt"), repository.list("").getEntries()
                .stream().map(entry -> entry.getName()).collect(java.util.stream.Collectors.toSet()));
        try (var files = Files.list(root)) {
            var storedNames = files.map(path -> path.getFileName().toString())
                    .filter(name -> !".frog2-upload-quota.lock".equals(name))
                    .toList();
            assertEquals(4, storedNames.size());
            assertTrue(storedNames.stream().allMatch(name ->
                    name.matches("\\.frog2-[0-9a-f]{32}\\.(data|meta)")));
        }
    }

    @Test
    void selectedImportRequiresAdminAndCsrfAndLeavesUnselectedFilesUntouched() throws Exception {
        Path selected = stableFile("selected.txt");
        Path unselected = stableFile("unselected.txt");
        FileRepositoryImportServlet importer = new FileRepositoryImportServlet(repository);
        String previousAdmins = System.getProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY);
        System.setProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, "admin-user");
        try {
            Request ordinary = authenticated("/file-repository/import", new Session("ordinary-user"));
            ordinary.parameterValues.put("selectedPath", new String[] {"selected.txt"});
            assertEquals(403, filtered(ordinary, importer::doPost).status);
            assertTrue(Files.exists(selected));

            Session admin = new Session("admin-user");
            Request invalidCsrf = new Request("/file-repository/import", admin);
            invalidCsrf.parameterValues.put("selectedPath", new String[] {"selected.txt"});
            assertEquals(403, filtered(invalidCsrf, importer::doPost).status);
            assertTrue(Files.exists(selected));

            Request allowed = authenticated("/file-repository/import", admin);
            allowed.parameterValues.put("selectedPath", new String[] {"selected.txt"});
            assertEquals(200, filtered(allowed, importer::doPost).status);
            assertFalse(Files.exists(selected));
            assertTrue(Files.exists(unselected));
            assertEquals(1, repository.list("").getFileCount());
            assertEquals("selected.txt", repository.list("").getEntries().getFirst().getName());
        } finally {
            if (previousAdmins == null) {
                System.clearProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY);
            } else {
                System.setProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, previousAdmins);
            }
        }
    }

    @Test
    void maintenanceWritesFollowSessionAssignmentsRatherThanCreatorOrDisplayName() throws Exception {
        StatefulMaintenanceDAO records = new StatefulMaintenanceDAO();
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Isolated Customer");
        customer.setManagerName("Same Name");
        customer.setSubManagerName("Same Name");
        customer.setCustomerType("정기점검 계약 고객사");
        customer.setVerticaVersion("25.4.0-9");
        customer.setLicenseSize("2TB");
        Set<String> assignees = new HashSet<>(Set.of("owner-user", "sub-user"));
        CustomerAssignmentDAO assignments = new CustomerAssignmentDAO() {
            @Override
            public List<CustomerDTO> getMaintenanceCustomersByAssignee(String userId, String name) {
                return assignees.contains(userId) ? List.of(customer) : List.of();
            }

            @Override
            public Set<String> getCustomerNamesByAssignee(String userId, String name) {
                return assignees.contains(userId) ? Set.of(customer.getCustomerName()) : Set.of();
            }
        };
        MaintenanceServlet servlet = new MaintenanceServlet(
                records, new CustomerDAO(), assignments, BusinessDate.systemClock());
        Session owner = new Session("owner-user");
        Request create = maintenanceRequest(owner, "add", "created");
        create.parameters.put("created_by_user_id", "forged-user");
        assertEquals(302, filtered(create, servlet::doPost).status);
        assertNotNull(records.record);
        assertEquals("owner-user", records.record.getCreatorUserId());
        assertEquals("25.4.0-9", records.record.getVerticaVersion());
        assertEquals("2", records.record.getLicenseSizeGb());

        Session attacker = new Session("attacker-user");
        for (String action : List.of("update", "delete")) {
            assertEquals(302, filtered(maintenanceRequest(attacker, action, "forged"), servlet::doPost).status);
            assertEquals("created", records.record.getNote());
        }
        assertEquals(0, records.updates);
        assertEquals(0, records.deletes);

        Request invalidCsrf = maintenanceRequest(owner, "update", "invalid-csrf");
        invalidCsrf.headers.put(CsrfToken.HEADER_NAME, attacker.csrf);
        int readsBefore = records.reads;
        assertEquals(403, filtered(invalidCsrf, servlet::doPost).status);
        assertEquals(readsBefore, records.reads);

        Session secondary = new Session("sub-user");
        assertEquals(302, filtered(maintenanceRequest(secondary, "update", "assigned-edit"), servlet::doPost).status);
        assertEquals("assigned-edit", records.record.getNote());
        assertEquals("owner-user", records.record.getCreatorUserId());
        assertEquals(1, records.updates);

        assignees.remove("owner-user");
        assertEquals(302, filtered(maintenanceRequest(owner, "delete", ""), servlet::doPost).status);
        assertNotNull(records.record);
        assertEquals(0, records.deletes);
        assertEquals(302, filtered(maintenanceRequest(secondary, "delete", ""), servlet::doPost).status);
        assertNull(records.record);
        assertEquals(1, records.deletes);
    }

    private FileRepositoryUploadServlet uploadServlet() throws Exception {
        FileRepositoryUploadServlet servlet = new FileRepositoryUploadServlet();
        Field field = FileRepositoryUploadServlet.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(servlet, repository);
        return servlet;
    }

    private Path stableFile(String name) throws Exception {
        Path path = Files.writeString(root.resolve(name), "isolated import content");
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(Duration.ofMinutes(1))));
        return path;
    }

    static Request authenticated(String path, Session session) {
        Request request = new Request(path, session);
        request.headers.put(CsrfToken.HEADER_NAME, session.csrf);
        return request;
    }

    static Request maintenanceRequest(Session session, String action, String note) {
        Request request = authenticated("/maintenance", session);
        request.parameters.putAll(Map.of(
                "action", action, "maintenance_id", "17", "customer_name", "Isolated Customer",
                "inspection_date", "2026-09-30", "inspector_name", "Same Name",
                "note", note, "license_usage_size", "1TB", "vertica_version", "forged-version"));
        return request;
    }

    static Response filtered(Request request, Handler handler) throws Exception {
        Response response = new Response();
        new AuthFilter().doFilter(request.proxy(), response.proxy(), (authenticated, authenticatedResponse) ->
                new CsrfFilter().doFilter(authenticated, authenticatedResponse, (validated, validatedResponse) ->
                        handler.handle((HttpServletRequest) validated, (HttpServletResponse) validatedResponse)));
        return response;
    }

    @FunctionalInterface
    interface Handler {
        void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
    }

    private static final class StatefulMaintenanceDAO extends MaintenanceRecordDAO {
        private MaintenanceRecordDTO record;
        private int reads;
        private int updates;
        private int deletes;

        @Override
        public boolean addMaintenanceRecord(MaintenanceRecordDTO record) {
            this.record = record;
            record.setMaintenanceId(17L);
            return true;
        }

        @Override
        public MaintenanceRecordDTO getMaintenanceRecordById(Long id) {
            reads++;
            return record;
        }

        @Override
        public boolean updateMaintenanceRecordForCustomer(MaintenanceRecordDTO update, String expectedCustomer) {
            assertEquals(record.getCustomerName(), expectedCustomer);
            updates++;
            record.setNote(update.getNote());
            return true;
        }

        @Override
        public boolean deleteMaintenanceRecordForCustomer(Long id, String expectedCustomer) {
            assertEquals(record.getCustomerName(), expectedCustomer);
            deletes++;
            record = null;
            return true;
        }
    }

    static final class Session {
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession proxy;
        final String csrf;

        Session(String userId) {
            this(new UserDTO(userId, "", "Same Name", "QA"));
        }

        Session(UserDTO user) {
            attributes.put("user", user);
            proxy = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class}, (ignored, call, args) -> switch (call.getName()) {
                        case "getAttribute" -> attributes.get((String) args[0]);
                        case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                        case "removeAttribute" -> { attributes.remove((String) args[0]); yield null; }
                        default -> defaultValue(call.getReturnType());
                    });
            csrf = CsrfToken.getOrCreate(proxy);
        }
    }

    static final class Request {
        private final String path;
        private final Session session;
        final Map<String, String> parameters = new HashMap<>();
        private final Map<String, String[]> parameterValues = new HashMap<>();
        final Map<String, String> headers = new HashMap<>(Map.of("Accept", "application/json"));
        private final Map<String, Object> attributes = new HashMap<>();
        private List<Part> parts = List.of();
        private int partReads;

        Request(String path, Session session) { this.path = path; this.session = session; }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class}, (ignored, call, args) -> switch (call.getName()) {
                        case "getMethod" -> "POST";
                        case "getRequestURI" -> "/frog2" + path;
                        case "getContextPath" -> "/frog2";
                        case "getSession" -> session == null ? null : session.proxy;
                        case "getHeader" -> headers.get((String) args[0]);
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getParameterValues" -> parameterValues.get((String) args[0]);
                        case "getAttribute" -> attributes.get((String) args[0]);
                        case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
                        case "removeAttribute" -> { attributes.remove((String) args[0]); yield null; }
                        case "getParts" -> { partReads++; yield parts; }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    static final class Response {
        int status = 200;
        final StringWriter body = new StringWriter();

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class}, (ignored, call, args) -> switch (call.getName()) {
                        case "getWriter" -> new PrintWriter(body);
                        case "setStatus", "sendError" -> { status = (Integer) args[0]; yield null; }
                        case "sendRedirect" -> { status = 302; yield null; }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class UploadPart {
        private final String name;
        private final byte[] content;
        private boolean deleted;

        private UploadPart(String name, String content) {
            this.name = name;
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        private Part proxy() {
            return (Part) Proxy.newProxyInstance(Part.class.getClassLoader(), new Class<?>[] {Part.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getName" -> "uploadFiles";
                        case "getSubmittedFileName" -> name;
                        case "getContentType" -> "text/plain";
                        case "getSize" -> (long) content.length;
                        case "getInputStream" -> new ByteArrayInputStream(content);
                        case "delete" -> { deleted = true; yield null; }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }
}
