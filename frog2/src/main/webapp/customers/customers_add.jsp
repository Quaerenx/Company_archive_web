<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="새 고객사 추가" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customers_add.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<!-- 전체를 customer-add-page 클래스로 감싸기 -->
<div class="customer-add-page customer-form-page customer-management content-management content-shell">
        <t:pageHeader>
            <jsp:attribute name="title">
                <i class="fas fa-plus-circle"></i> 새 고객사 등록
            </jsp:attribute>
            <jsp:attribute name="subtitle">
                새로운 고객사의 기본 정보를 입력해주세요.
            </jsp:attribute>
            <jsp:attribute name="actions">
                <a href="${pageContext.request.contextPath}/customers?view=list"
                   class="ui-button button--secondary button--md">
                    <i class="fas fa-list"></i> 목록으로
                </a>
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
        
        <!-- 등록 폼 -->
        <div class="form-container ui-form-card">
            <form class="ui-form ui-form-layout"
                  method="post"
                  action="${pageContext.request.contextPath}/customers"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">

                <c:set var="customerFormMode" value="add" />
                <%@ include file="/WEB-INF/includes/_customer_form_fields.jspf" %>
                
                <!-- 버튼 -->
                <div class="button-group ui-form-actions">
                    <a href="${pageContext.request.contextPath}/customers?view=list"
                       class="ui-button button--secondary button--md">취소</a>
                    <button type="submit"
                            class="ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
</div>


<%@ include file="/includes/footer.jsp" %>
