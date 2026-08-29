package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FileRepositoryWebConfigurationTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void webXmlDefinesServletOnlyUploadAndExplicitLimits() throws Exception {
        String webXml = Files.readString(WEBAPP.resolve("WEB-INF/web.xml"));

        assertTrue(webXml.contains("com.company.controller.FileRepositoryServlet"));
        assertTrue(webXml.contains("com.company.controller.FileRepositoryUploadServlet"));
        assertTrue(webXml.contains("com.company.controller.FileRepositoryDownloadServlet"));
        assertTrue(webXml.contains("com.company.controller.FileRepositoryImportServlet"));
        assertTrue(webXml.contains("/file-repository/import"));
        assertTrue(webXml.contains("<max-file-size>10485760</max-file-size>"));
        assertTrue(webXml.contains("<max-request-size>53477376</max-request-size>"));
        assertTrue(webXml.contains("/filerepo/filerepo_downlist.jsp"));
        assertTrue(webXml.contains("/filerepo/filerepo_uploadProcess.jsp"));
        assertTrue(webXml.contains("/filerepo/filerepo_download.jsp"));
    }

    @Test
    void repositoryViewsArePrivateAndLegacyUploadJspsAreAbsent() {
        assertTrue(Files.isRegularFile(WEBAPP.resolve("WEB-INF/views/filerepo/list.jsp")));
        assertTrue(Files.isRegularFile(WEBAPP.resolve("WEB-INF/views/filerepo/upload.jsp")));
        assertFalse(Files.exists(WEBAPP.resolve("filerepo/filerepo_upload.jsp")));
        assertFalse(Files.exists(WEBAPP.resolve("filerepo/filerepo_uploadProcess.jsp")));
        assertFalse(Files.exists(WEBAPP.resolve("filerepo/filerepo_download.jsp")));
        assertFalse(Files.exists(WEBAPP.resolve("files")));
    }

    @Test
    void uploadViewUsesAnExternalScript() throws Exception {
        String upload = Files.readString(
                WEBAPP.resolve("WEB-INF/views/filerepo/upload.jsp"));
        String script = Files.readString(
                WEBAPP.resolve("resources/js/pages/file_repository_upload.js"));

        assertTrue(upload.contains("/resources/js/pages/file_repository_upload.js"));
        assertFalse(upload.contains("<script>"));
        assertFalse(script.contains(Character.toString(36) + "{"));
        assertTrue(script.contains("input[name=\"_csrf\"]"));
        assertTrue(script.contains("'X-CSRF-Token': csrfInput.value"));
    }

    @Test
    void browserPageGetErrorsUseTheCommonResponseContract() throws Exception {
        String listing = Files.readString(Path.of(
                "src/main/java/com/company/controller/FileRepositoryServlet.java"));
        String upload = Files.readString(Path.of(
                "src/main/java/com/company/controller/FileRepositoryUploadServlet.java"));
        String uploadGet = upload.substring(0, upload.indexOf("protected void doPost"));
        String uploadPost = upload.substring(upload.indexOf("protected void doPost"));

        assertTrue(listing.contains("ApplicationError.send"));
        assertFalse(listing.contains("FileRepositoryJson.sendError"));
        assertTrue(uploadGet.contains("ApplicationError.send"));
        assertFalse(uploadGet.contains("FileRepositoryJson.sendError"));
        assertTrue(uploadPost.contains("FileRepositoryJson.sendError"));
    }

    @Test
    void commonHtmlErrorsCoverBadRequestsAndUnsupportedMethods() throws Exception {
        String webXml = Files.readString(WEBAPP.resolve("WEB-INF/web.xml"));

        assertTrue(webXml.contains("<error-code>400</error-code>"));
        assertTrue(webXml.contains("<location>/error/400.jsp</location>"));
        assertTrue(webXml.contains("<error-code>405</error-code>"));
        assertTrue(webXml.contains("<location>/error/405.jsp</location>"));
        assertTrue(Files.isRegularFile(WEBAPP.resolve("error/400.jsp")));
        assertTrue(Files.isRegularFile(WEBAPP.resolve("error/405.jsp")));
    }

    @Test
    void globalCsrfFilterOwnsUploadProtection() throws Exception {
        String webXml = Files.readString(WEBAPP.resolve("WEB-INF/web.xml"));
        String upload = Files.readString(Path.of(
                "src/main/java/com/company/controller/FileRepositoryUploadServlet.java"));

        assertTrue(webXml.contains("com.company.security.CsrfFilter"));
        assertFalse(upload.contains("FileRepositoryCsrf"));
        assertFalse(upload.contains("CsrfToken.isValid"));
        assertFalse(Files.exists(Path.of(
                "src/main/java/com/company/filerepo/FileRepositoryCsrf.java")));
    }

    @Test
    void administratorImportAndCorruptionHealthStayExplicitInThePrivateView()
            throws Exception {
        String listing = Files.readString(
                WEBAPP.resolve("WEB-INF/views/filerepo/list.jsp"));
        String servlet = Files.readString(Path.of(
                "src/main/java/com/company/controller/FileRepositoryImportServlet.java"));

        assertTrue(listing.contains("${fileRepositoryAdmin}"));
        assertTrue(listing.contains("${listing.invalidEntryCount}"));
        assertTrue(listing.contains("name=\"_csrf\""));
        assertTrue(listing.contains("data-ui-submit-lock=\"auto\""));
        assertTrue(listing.contains("data-busy-label=\"반입 중\""));
        assertTrue(listing.contains("서버 복사가 끝난 뒤 30초"));
        assertTrue(servlet.contains("AdminAccessPolicy.isAdmin"));
        assertTrue(servlet.contains("SessionPrincipal.from(request)"));
        assertTrue(servlet.contains("service.importUnmanaged"));
    }
}
