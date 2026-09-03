<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="회의록 작성" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-meeting page-meeting-form" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="meetingListReturnUrl" value="/meeting">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnQ}"><c:param name="q" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnType}"><c:param name="type" value="${param.returnType}" /></c:if>
    <c:if test="${not empty param.returnAuthor}"><c:param name="author" value="${param.returnAuthor}" /></c:if>
    <c:if test="${not empty param.returnStartDate}"><c:param name="startDate" value="${param.returnStartDate}" /></c:if>
    <c:if test="${not empty param.returnEndDate}"><c:param name="endDate" value="${param.returnEndDate}" /></c:if>
</c:url>

<div class="meeting-page-container content-management content-shell" data-meeting-mode="write">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-pen"></i> 새 회의록 작성</jsp:attribute>
        <jsp:attribute name="subtitle">회의 내용을 정리하여 등록해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${meetingListReturnUrl}' />"
               class="ui-button button--secondary button--md"><i class="fas fa-list"></i> 목록으로</a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
        </div>
    </c:if>

    <!-- 작성 폼 -->
    <div class="ui-form-card">
        <form method="post"
              action="${pageContext.request.contextPath}/meeting"
              id="meetingForm"
              class="ui-form ui-form-layout"
              data-ui-draft="auto"
              data-ui-draft-id="meeting:new"
              data-ui-draft-success-views="list,view"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="write">
            <c:if test="${not empty param.returnPage}"><input type="hidden" name="returnPage" value="<c:out value='${param.returnPage}' />" /></c:if>
            <c:if test="${not empty param.returnQ}"><input type="hidden" name="returnQ" value="<c:out value='${param.returnQ}' />" /></c:if>
            <c:if test="${not empty param.returnType}"><input type="hidden" name="returnType" value="<c:out value='${param.returnType}' />" /></c:if>
            <c:if test="${not empty param.returnAuthor}"><input type="hidden" name="returnAuthor" value="<c:out value='${param.returnAuthor}' />" /></c:if>
            <c:if test="${not empty param.returnStartDate}"><input type="hidden" name="returnStartDate" value="<c:out value='${param.returnStartDate}' />" /></c:if>
            <c:if test="${not empty param.returnEndDate}"><input type="hidden" name="returnEndDate" value="<c:out value='${param.returnEndDate}' />" /></c:if>

            <c:set var="meetingFormMode" value="write" />
            <%@ include file="/WEB-INF/includes/_meeting_form_fields.jspf" %>

            <!-- 버튼 -->
            <div class="button-group ui-form-actions">
                <a href="<c:out value='${meetingListReturnUrl}' />"
                   class="ui-button button--secondary button--md">취소</a>
                <button type="button"
                        class="ui-button button--secondary button--md"
                        data-meeting-action="preview">미리보기</button>
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="등록 중">등록하기</button>
            </div>
        </form>
    </div>
</div>

<!-- 미리보기 모달 -->
<div id="previewModal"
     class="modal modal-wide"
     role="dialog"
     aria-modal="true"
     aria-labelledby="previewModalTitle"
     aria-hidden="true"
     tabindex="-1">
    <div class="modal-content">
        <div class="modal-header">
            <h3 id="previewModalTitle"><i class="fas fa-eye"></i> 미리보기</h3>
            <button type="button"
                    class="modal-close ui-touch-target"
                    data-meeting-action="close-preview"
                    data-dialog-initial-focus
                    aria-label="미리보기 닫기">&times;</button>
        </div>
        <div class="modal-body">
            <div class="preview-content">
                <div class="preview-meta">
                    <h2 id="preview-title"></h2>
                    <div class="preview-info">
                        <span class="preview-type"></span>
                        <span class="preview-datetime"></span>
                        <span class="preview-author">작성자: <c:out value="${user.userName}" /></span>
                    </div>
                </div>
                <div class="preview-text" id="preview-content"></div>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button"
                    class="ui-button button--secondary button--md"
                    data-meeting-action="close-preview">닫기</button>
        </div>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
