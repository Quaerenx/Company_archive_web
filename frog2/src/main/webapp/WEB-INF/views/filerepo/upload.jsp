<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="업무자료 업로드" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-file-upload" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/upload.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/file_repository_upload.js" scope="request" />
<c:url var="uploadEndpoint" value="/file-repository/upload" />
<c:url var="listUrl" value="/file-repository">
    <c:param name="path" value="${listing.currentPath}" />
</c:url>

<%@ include file="/includes/header.jsp" %>

<div class="upload-page content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-cloud-upload-alt" aria-hidden="true"></i> 업무자료 업로드</jsp:attribute>
        <jsp:attribute name="subtitle">최대 5개, 파일당 10 MB까지 업로드할 수 있습니다.</jsp:attribute>
    </t:pageHeader>

    <section class="upload-container" aria-label="업무자료 파일 선택">

        <form id="file-upload-form"
              class="ui-form"
              action="<c:out value="${uploadEndpoint}" />"
              method="post"
              enctype="multipart/form-data"
              data-ui-submit-lock="manual"
              data-success-url="<c:out value="${listUrl}" />">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="path" value="<c:out value="${listing.currentPath}" />">

            <label class="upload-area" for="upload-files">
                <span class="upload-area-icon" aria-hidden="true"><i class="fas fa-file-upload"></i></span>
                <span class="upload-area-text">파일을 선택하세요</span>
                <span class="upload-area-hint">PDF, 문서, 스프레드시트, 안전한 이미지·압축·텍스트 파일</span>
                <input id="upload-files"
                       class="file-input"
                       type="file"
                       name="uploadFiles"
                       multiple
                       required
                       accept=".pdf,.txt,.log,.csv,.png,.jpg,.jpeg,.gif,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.7z,.rar,.gz,.tar">
            </label>

            <div id="selected-files" class="file-list" aria-live="polite"></div>
            <p id="upload-status"
               class="progress-text ui-status ui-status--neutral"
               role="status"
               aria-live="polite"
               aria-atomic="true"></p>

            <div class="upload-actions">
                <a class="upload-button ui-button button--secondary button--md"
                   href="<c:out value="${listUrl}" />">목록으로</a>
                <button id="upload-button"
                        class="upload-button ui-button button--primary button--md"
                        type="submit">업로드</button>
            </div>
        </form>
    </section>
</div>

<%@ include file="/includes/footer.jsp" %>
