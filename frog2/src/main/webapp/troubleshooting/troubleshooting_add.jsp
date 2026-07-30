<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="트러블 슈팅 등록" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-troubleshooting" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/troubleshooting_form.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/troubleshooting_form.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<!-- 전체를 add-page 클래스로 감싸기 -->
<div class="troubleshooting-form-page" data-troubleshooting-form-mode="add">
    <div class="container content-shell">
        <div class="page-header">
            <h2><i class="fas fa-plus-circle"></i> 새 트러블 슈팅 등록</h2>
            <p>기술지원 및 문제 해결 정보를 입력해주세요.</p>
        </div>

        <!-- 오류 메시지 -->
        <c:if test="${not empty error}">
            <div class="alert alert-danger ui-alert ui-alert--danger"
                 role="alert"
                 aria-atomic="true">
                <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
            </div>
        </c:if>

        <!-- 등록 폼 -->
        <div class="form-container ui-form-card">
            <form id="troubleshootingForm"
                  class="ui-form ui-form-layout"
                  method="post"
                  action="${pageContext.request.contextPath}/troubleshooting"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">

                <c:set var="troubleshootingFormMode" value="add" />
                <%@ include file="/WEB-INF/includes/_troubleshooting_form_fields.jspf" %>

                <!-- 버튼 -->
                <div class="button-group">
                    <a href="${pageContext.request.contextPath}/troubleshooting?view=list"
                       class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                    <button type="submit"
                            class="btn btn-primary ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
