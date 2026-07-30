<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="트러블 슈팅 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_form.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<div class="troubleshooting-form-page" data-troubleshooting-form-mode="edit">
    <div class="container content-shell">
        <div class="page-header">
            <h2><i class="fas fa-edit"></i> 트러블 슈팅 수정</h2>
            <p>"<strong><c:out value="${troubleshooting.title}" /></strong>" 정보를 수정합니다.</p>
        </div>

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
        <form id="troubleshootingForm"
              class="ui-form ui-form-layout"
              method="post"
              action="${pageContext.request.contextPath}/troubleshooting"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="id" value="${troubleshooting.id}">

            <c:set var="troubleshootingFormMode" value="edit" />
            <%@ include file="/WEB-INF/includes/_troubleshooting_form_fields.jspf" %>

            <!-- 버튼 -->
            <div class="button-group">
                <a href="${pageContext.request.contextPath}/troubleshooting?view=list"
                   class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                <a href="${pageContext.request.contextPath}/troubleshooting?view=view&id=${troubleshooting.id}"
                   class="btn btn-secondary ui-button button--secondary button--md">상세보기</a>
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        data-busy-label="수정 중">수정 완료</button>
            </div>
        </form>
    </div>
</div>
</div>



<%@ include file="/includes/footer.jsp" %>
