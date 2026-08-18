<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="트러블 슈팅 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_form.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<c:url var="troubleshootingListReturnUrl" value="/troubleshooting">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnQ}"><c:param name="q" value="${param.returnQ}" /></c:if>
    <c:if test="${param.returnScope eq 'content'}"><c:param name="scope" value="content" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="pageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="troubleshootingDetailReturnUrl" value="/troubleshooting">
    <c:param name="view" value="view" />
    <c:param name="id" value="${troubleshooting.id}" />
    <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
    <c:if test="${param.returnScope eq 'content'}"><c:param name="returnScope" value="content" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
</c:url>

<div class="troubleshooting-form-page" data-troubleshooting-form-mode="edit">
    <div class="container content-shell">
        <t:pageHeader>
            <jsp:attribute name="title"><i class="fas fa-edit" aria-hidden="true"></i> 트러블 슈팅 수정</jsp:attribute>
            <jsp:attribute name="subtitle"><strong><c:out value="${troubleshooting.title}" /></strong> 정보를 수정합니다.</jsp:attribute>
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
            <div class="button-group ui-form-actions">
                <a href="<c:out value='${troubleshootingListReturnUrl}' />"
                   class="ui-button button--secondary button--md">취소</a>
                <a href="<c:out value='${troubleshootingDetailReturnUrl}' />"
                   class="ui-button button--secondary button--md">상세보기</a>
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="수정 중">수정 완료</button>
            </div>
        </form>
    </div>
</div>
</div>



<%@ include file="/includes/footer.jsp" %>
