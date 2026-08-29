package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.filerepo.FileRepositoryService;
import com.company.model.UserDTO;
import com.company.security.AdminAccessPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryImportServletTest {
    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void clearAdminConfiguration() {
        System.clearProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY);
    }

    @Test
    void administratorCanImportStableServerFile() throws Exception {
        Path root = Files.createDirectory(
                temporaryDirectory.resolve("repository"));
        Path copied = Files.writeString(root.resolve("manual.txt"), "manual");
        Files.setLastModifiedTime(
                copied,
                FileTime.from(Instant.now().minus(Duration.ofMinutes(1))));
        FileRepositoryService service = new FileRepositoryService(root);
        FileRepositoryImportServlet servlet =
                new FileRepositoryImportServlet(service);
        System.setProperty(
                AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, "admin-user");
        SessionFixture session = new SessionFixture(
                new UserDTO("admin-user", "", "Admin", "QA"));
        RequestFixture request = new RequestFixture(session);
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertNotNull(response.redirect);
        assertTrue(response.redirect.startsWith(
                "/frog2/file-repository?_flash="));
        assertEquals(1, service.list("").getFileCount());
    }

    @Test
    void nonAdministratorCannotStartServerImport() throws Exception {
        Path root = Files.createDirectory(
                temporaryDirectory.resolve("repository-forbidden"));
        FileRepositoryImportServlet servlet =
                new FileRepositoryImportServlet(new FileRepositoryService(root));
        System.setProperty(
                AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, "admin-user");
        RequestFixture request = new RequestFixture(new SessionFixture(
                new UserDTO("ordinary-user", "", "User", "QA")));
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status.get());
    }

    private static final class RequestFixture {
        private final SessionFixture session;

        private RequestFixture(SessionFixture session) {
            this.session = session;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "getParameter" -> "path".equals(args[0]) ? "" : null;
                        case "getContextPath" -> "/frog2";
                        case "getSession" -> session.proxy();
                        case "getAttribute" -> null;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static final class SessionFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private HttpSession proxy;

        private SessionFixture(UserDTO user) {
            attributes.put("user", user);
        }

        private HttpSession proxy() {
            if (proxy == null) {
                proxy = (HttpSession) Proxy.newProxyInstance(
                        HttpSession.class.getClassLoader(),
                        new Class<?>[] {HttpSession.class},
                        (ignored, method, args) -> switch (method.getName()) {
                            case "getAttribute" -> attributes.get(args[0]);
                            case "setAttribute" -> {
                                attributes.put((String) args[0], args[1]);
                                yield null;
                            }
                            case "removeAttribute" -> {
                                attributes.remove(args[0]);
                                yield null;
                            }
                            default -> defaultValue(method.getReturnType());
                        });
            }
            return proxy;
        }
    }

    private static final class ResponseFixture {
        private final AtomicInteger status = new AtomicInteger(200);
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "sendError" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

}
