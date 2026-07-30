<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="업무자료" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-file-repository file-server-container" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/download.css" scope="request" />
<c:url var="uploadUrl" value="/file-repository/upload">
    <c:param name="path" value="${listing.currentPath}" />
</c:url>

<%@ include file="/includes/header.jsp" %>

<main class="main-content content-shell">
    <section class="file-main" aria-labelledby="repository-title">
        <div class="upload-section">
            <div class="upload-info">
                <h5 id="repository-title"><i class="fas fa-folder-open"></i> 업무자료</h5>
                <small>안전한 외부 저장소에서 제공되는 파일입니다.</small>
            </div>
            <a class="upload-btn ui-button button--primary button--md"
               href="<c:out value="${uploadUrl}" />">
                <i class="fas fa-upload"></i> 파일 업로드
            </a>
        </div>

        <nav class="breadcrumb" aria-label="자료실 경로">
            <c:url var="rootUrl" value="/file-repository" />
            <a href="<c:out value="${rootUrl}" />"><i class="fas fa-home"></i> 자료실</a>
            <c:forEach var="crumb" items="${listing.breadcrumbs}">
                <span aria-hidden="true"> / </span>
                <c:url var="crumbUrl" value="/file-repository">
                    <c:param name="path" value="${crumb.path}" />
                </c:url>
                <a href="<c:out value="${crumbUrl}" />"><c:out value="${crumb.name}" /></a>
            </c:forEach>
        </nav>

        <div class="table-wrapper ui-table-wrap">
            <table class="file-table ui-table">
                <thead>
                    <tr>
                        <th scope="col">이름</th>
                        <th scope="col">설명</th>
                        <th scope="col">수정일</th>
                        <th scope="col" class="size">크기</th>
                    </tr>
                </thead>
                <tbody>
                    <c:if test="${not empty listing.currentPath}">
                        <c:url var="parentUrl" value="/file-repository">
                            <c:param name="path" value="${listing.parentPath}" />
                        </c:url>
                        <tr class="parent-dir">
                            <td colspan="4">
                                <a href="<c:out value="${parentUrl}" />"><i class="fas fa-level-up-alt"></i> 상위 폴더</a>
                            </td>
                        </tr>
                    </c:if>

                    <c:forEach var="entry" items="${listing.entries}">
                        <tr>
                            <td class="file-name ${entry.directory ? 'directory' : ''}">
                                <span class="icon" aria-hidden="true"><c:out value="${entry.icon}" /></span>
                                <c:choose>
                                    <c:when test="${entry.directory}">
                                        <c:url var="directoryUrl" value="/file-repository">
                                            <c:param name="path" value="${entry.path}" />
                                        </c:url>
                                        <a href="<c:out value="${directoryUrl}" />"><c:out value="${entry.name}" /></a>
                                    </c:when>
                                    <c:otherwise>
                                        <c:url var="downloadUrl" value="/file-repository/download">
                                            <c:param name="path" value="${entry.path}" />
                                            <c:param name="id" value="${entry.id}" />
                                        </c:url>
                                        <a href="<c:out value="${downloadUrl}" />" rel="nofollow"><c:out value="${entry.name}" /></a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${entry.description}" /></td>
                            <td class="date"><c:out value="${entry.lastModifiedText}" /></td>
                            <td class="size"><c:out value="${entry.sizeText}" /></td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty listing.entries and empty listing.currentPath}">
                        <tr>
                            <td colspan="4" class="empty-state">등록된 파일이 없습니다.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <div class="stats">
            폴더 <strong><c:out value="${listing.directoryCount}" /></strong>개,
            파일 <strong><c:out value="${listing.fileCount}" /></strong>개,
            합계 <strong><c:out value="${listing.totalSizeText}" /></strong>
        </div>
    </section>
</main>

<%@ include file="/includes/footer.jsp" %>
