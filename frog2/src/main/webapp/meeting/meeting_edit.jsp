<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="회의록 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-meeting page-meeting-form" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="meetingDetailReturnUrl" value="/meeting">
    <c:param name="view" value="view" />
    <c:param name="id" value="${meeting.meetingId}" />
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
</c:url>

<div class="meeting-page-container content-management content-shell" data-meeting-mode="edit">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-edit"></i> 회의록 수정</jsp:attribute>
        <jsp:attribute name="subtitle">회의 내용을 수정해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${meetingDetailReturnUrl}' />"
               class="ui-button button--secondary button--md"><i class="fas fa-file-alt"></i> 상세보기</a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="alert alert-danger ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
        </div>
    </c:if>

    <!-- 수정 폼 -->
    <div class="form-container ui-form-card">
        <form method="post"
              action="${pageContext.request.contextPath}/meeting"
              id="meetingForm"
              class="ui-form ui-form-layout"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="meeting_id" value="${meeting.meetingId}">

            <c:set var="meetingFormMode" value="edit" />
            <%@ include file="/WEB-INF/includes/_meeting_form_fields.jspf" %>

            <!-- 작성 정보 -->
            <div class="section-title">작성 정보</div>
            <fmt:formatDate var="meetingCreatedIso" value="${meeting.createdAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" />
            <fmt:formatDate var="meetingCreatedLabel" value="${meeting.createdAt}" pattern="yyyy-MM-dd HH:mm" />
            <fmt:formatDate var="meetingUpdatedIso" value="${meeting.updatedAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" />
            <fmt:formatDate var="meetingUpdatedLabel" value="${meeting.updatedAt}" pattern="yyyy-MM-dd HH:mm" />
            <dl class="meeting-audit-list">
                <div>
                    <dt>작성자</dt>
                    <dd><c:out value="${meeting.authorName}" /></dd>
                </div>
                <div>
                    <dt>등록</dt>
                    <dd><time datetime="<c:out value='${meetingCreatedIso}' />"><c:out value="${meetingCreatedLabel}" /></time></dd>
                </div>
                <c:if test="${meeting.updatedAt != meeting.createdAt}">
                    <div>
                        <dt>최종 수정</dt>
                        <dd><time datetime="<c:out value='${meetingUpdatedIso}' />"><c:out value="${meetingUpdatedLabel}" /></time></dd>
                    </div>
                </c:if>
            </dl>

            <!-- 버튼 -->
            <div class="button-group ui-form-actions">
                <a href="<c:out value='${meetingDetailReturnUrl}' />"
                   class="ui-button button--secondary button--md">취소</a>
                <button type="button"
                        class="ui-button button--secondary button--md"
                        data-meeting-action="preview">미리보기</button>
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="수정 중">수정하기</button>
                <button type="button"
                        class="ui-button button--danger button--md meeting-delete-button"
                        data-meeting-action="delete">삭제하기</button>
            </div>
        </form>

        <!-- 삭제 폼 (숨김) -->
        <form id="deleteForm"
              method="post"
              action="${pageContext.request.contextPath}/meeting"
              class="d-none ui-form">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="meeting_id" value="${meeting.meetingId}">
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
                        <span class="preview-author">작성자: <c:out value="${meeting.authorName}" /></span>
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
