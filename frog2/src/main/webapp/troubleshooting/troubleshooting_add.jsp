<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="트러블 슈팅 등록" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_form.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<!-- 전체를 add-page 클래스로 감싸기 -->
<div class="troubleshooting-form-page" data-troubleshooting-form-mode="add">
    <div class="container content-shell">
        <t:pageHeader>
            <jsp:attribute name="title"><i class="fas fa-plus-circle" aria-hidden="true"></i> 새 트러블 슈팅 등록</jsp:attribute>
            <jsp:attribute name="subtitle">기술지원 및 문제 해결 정보를 입력해 주세요.</jsp:attribute>
        </t:pageHeader>

        <!-- 오류 메시지 -->
        <c:if test="${not empty error}">
            <div class="ui-alert ui-alert--danger"
                 role="alert"
                 aria-atomic="true">
                <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
            </div>
        </c:if>

        <!-- 등록 폼 -->
        <div class="ui-form-card">
            <form id="troubleshootingForm"
                  class="ui-form ui-form-layout"
                  method="post"
                  action="${pageContext.request.contextPath}/troubleshooting"
                  data-ui-dirty-guard="auto"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">

                <c:set var="troubleshootingFormMode" value="add" />
                <%@ include file="/WEB-INF/includes/_troubleshooting_form_fields.jspf" %>

                <!-- 버튼 -->
                <div class="button-group ui-form-actions">
                    <a href="${pageContext.request.contextPath}/troubleshooting?view=list"
                       class="ui-button button--secondary button--md">취소</a>
                    <button type="submit"
                            class="ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
