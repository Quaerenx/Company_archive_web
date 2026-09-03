<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="서버 파일 반입" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-file-import" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/download.css,/resources/css/pages/file_repository_import.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/file_repository_import.js" scope="request" />
<c:url var="importEndpoint" value="/file-repository/import" />

<%@ include file="/includes/header.jsp" %>

<div class="file-import-page content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-file-import" aria-hidden="true"></i> 서버 파일 반입</jsp:attribute>
        <jsp:attribute name="subtitle">서버에 직접 복사한 파일을 확인한 뒤 선택해 자료실에 등록합니다.</jsp:attribute>
        <jsp:attribute name="actions">
            <a class="ui-button button--secondary button--md"
               href="<c:out value='${listingUrl}' />">
                <i class="fas fa-arrow-left" aria-hidden="true"></i> 자료실로
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <section class="file-import-summary ui-work-surface" aria-labelledby="import-summary-title">
        <div class="file-import-summary__heading">
            <div>
                <h2 id="import-summary-title">반입 전 확인</h2>
                <p class="text-muted">
                    경로: <strong><c:out value="${empty importPreview.relativePath ? '자료실 루트' : importPreview.relativePath}" /></strong>
                </p>
            </div>
            <p class="file-import-summary__notice">
                파일 복사가 끝난 뒤 30초가 지난 항목만 반입할 수 있습니다.
            </p>
        </div>
        <dl class="file-import-counts" aria-label="반입 대상 상태 요약">
            <div><dt>반입 가능</dt><dd><c:out value="${importPreview.readyCount}" /></dd></div>
            <div><dt>이름 충돌</dt><dd><c:out value="${importPreview.conflictCount}" /></dd></div>
            <div><dt>거부</dt><dd><c:out value="${importPreview.rejectedCount}" /></dd></div>
            <div><dt>안정화 대기</dt><dd><c:out value="${importPreview.deferredCount}" /></dd></div>
            <div><dt>확인 실패</dt><dd><c:out value="${importPreview.failedCount}" /></dd></div>
        </dl>
    </section>

    <form id="file-import-form"
          class="file-import-form ui-work-surface ui-form"
          action="<c:out value='${importEndpoint}' />"
          method="post"
          data-ui-submit-lock="manual">
        <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
        <input type="hidden" name="path" value="<c:out value='${importPreview.relativePath}' />">

        <div class="file-import-toolbar ui-table-toolbar">
            <label class="file-import-select-all">
                <input id="import-select-all"
                       type="checkbox"
                       <c:if test="${importPreview.readyCount gt 0}">checked</c:if>
                       <c:if test="${importPreview.readyCount eq 0}">disabled</c:if>>
                반입 가능 파일 전체 선택
            </label>
            <span id="import-selected-count" class="text-muted" aria-live="polite"></span>
        </div>

        <c:choose>
            <c:when test="${empty importPreview.items}">
                <div class="ui-empty-state">
                    <i class="fas fa-folder-open" aria-hidden="true"></i>
                    <strong>반입할 서버 파일이 없습니다.</strong>
                    <span>일반 파일을 현재 자료실 경로에 복사한 뒤 다시 확인해 주세요.</span>
                </div>
            </c:when>
            <c:otherwise>
                <div class="ui-table-wrap" data-ui-scroll-region data-ui-scroll-label="서버 파일 반입 미리보기 표">
                    <table class="ui-table ui-data-table file-import-table">
                        <caption class="sr-only">서버 파일 반입 대상과 상태</caption>
                        <thead>
                            <tr>
                                <th scope="col" class="file-import-table__select">선택</th>
                                <th scope="col">파일</th>
                                <th scope="col" class="file-import-table__status">상태</th>
                                <th scope="col">사유</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${importPreview.items}">
                                <c:set var="badgeTone" value="neutral" />
                                <c:if test="${item.status eq 'ready'}"><c:set var="badgeTone" value="success" /></c:if>
                                <c:if test="${item.status eq 'conflict'}"><c:set var="badgeTone" value="warning" /></c:if>
                                <c:if test="${item.status eq 'rejected' or item.status eq 'failed'}"><c:set var="badgeTone" value="danger" /></c:if>
                                <tr>
                                    <td data-label="선택" class="file-import-table__select">
                                        <input type="checkbox"
                                               name="selectedPath"
                                               value="<c:out value='${item.path}' />"
                                               aria-label="<c:out value='${item.name}' /> 반입 선택"
                                               <c:if test="${item.selectable}">checked</c:if>
                                               <c:if test="${not item.selectable}">disabled</c:if>>
                                    </td>
                                    <td data-label="파일" class="file-import-table__name">
                                        <strong><c:out value="${item.name}" /></strong>
                                        <c:if test="${item.path ne item.name}">
                                            <span class="text-muted"><c:out value="${item.path}" /></span>
                                        </c:if>
                                    </td>
                                    <td data-label="상태" class="file-import-table__status">
                                        <span class="ui-badge ui-badge--${badgeTone}"><c:out value="${item.statusLabel}" /></span>
                                    </td>
                                    <td data-label="사유"><c:out value="${item.reason}" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>

        <div class="file-import-progress" aria-live="polite">
            <progress id="import-progress"
                      aria-label="서버 파일 반입 진행 상태"
                      hidden></progress>
            <p id="import-status" class="ui-status ui-status--neutral" role="status" aria-atomic="true"></p>
        </div>

        <section id="import-result" class="file-import-result" aria-labelledby="import-result-title" hidden>
            <div class="file-import-result__heading">
                <h2 id="import-result-title">반입 결과</h2>
                <div class="file-import-result__actions">
                    <button id="import-retry-button"
                            class="ui-button button--secondary button--sm"
                            type="button"
                            hidden>
                        <i class="fas fa-redo" aria-hidden="true"></i> 실패 항목만 다시 시도
                    </button>
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${listingUrl}' />">자료실에서 확인</a>
                </div>
            </div>
            <p id="import-result-summary" class="text-muted"></p>
            <div class="ui-table-wrap" data-ui-scroll-region data-ui-scroll-label="서버 파일 반입 결과 표">
                <table class="ui-table ui-data-table file-import-table">
                    <caption class="sr-only">서버 파일별 반입 결과</caption>
                    <thead>
                        <tr><th scope="col">파일</th><th scope="col">상태</th><th scope="col">결과</th></tr>
                    </thead>
                    <tbody id="import-result-body"></tbody>
                </table>
            </div>
        </section>

        <div class="file-import-actions">
            <a class="ui-button button--secondary button--md" href="<c:out value='${listingUrl}' />">취소</a>
            <button id="import-submit-button"
                    class="ui-button button--primary button--md"
                    type="submit"
                    <c:if test="${importPreview.readyCount eq 0}">disabled</c:if>>선택 파일 반입</button>
        </div>
    </form>
</div>

<%@ include file="/includes/footer.jsp" %>
