<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="회의록 작성" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-meeting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/meeting.css,/resources/css/pages/meeting_form.css,/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/meeting_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="meeting-page-container customer-management content-shell" data-meeting-mode="write">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-pen"></i> 새 회의록 작성</jsp:attribute>
        <jsp:attribute name="subtitle">회의 내용을 정리하여 등록해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/meeting?view=list"
               class="add-button secondary ui-button button--secondary button--md"><i class="fas fa-list"></i> 목록으로</a>
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

    <!-- 작성 폼 -->
    <div class="form-container ui-form-card">
        <form method="post"
              action="${pageContext.request.contextPath}/meeting"
              id="meetingForm"
              class="ui-form ui-form-layout"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="write">

            <c:set var="meetingFormMode" value="write" />
            <%@ include file="/WEB-INF/includes/_meeting_form_fields.jspf" %>

            <!-- 버튼 -->
            <div class="button-group">
                <a href="${pageContext.request.contextPath}/meeting?view=list"
                   class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                <button type="button"
                        class="btn btn-secondary ui-button button--secondary button--md"
                        data-meeting-action="preview">미리보기</button>
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        data-busy-label="등록 중">등록하기</button>
            </div>
        </form>
    </div>
</div>

<!-- 미리보기 모달 -->
<div id="previewModal" class="modal modal-wide">
    <div class="modal-content">
        <div class="modal-header">
            <h3><i class="fas fa-eye"></i> 미리보기</h3>
            <button type="button"
                    class="modal-close ui-touch-target"
                    data-meeting-action="close-preview"
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
                    class="btn btn-secondary ui-button button--secondary button--md"
                    data-meeting-action="close-preview">닫기</button>
        </div>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
