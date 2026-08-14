<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="고객사 정보 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customers_edit.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:url var="customerDetailUrl" value="/customers">
    <c:param name="view" value="detail" />
    <c:param name="customerName" value="${customer.customerName}" />
</c:url>

<%@ include file="/includes/header.jsp" %>

<!-- 목록 페이지의 레이아웃 감각 적용: 폭/여백/타이포 일치 -->
<div class="customer-edit-page customer-form-page customer-management content-management content-shell">
    <!-- 공통 페이지 헤더 컴포넌트 사용 -->
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-edit"></i> 고객사 정보 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            "<strong><c:out value="${customer.customerName}" /></strong>" 고객사의 정보를 수정합니다.
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/customers?view=list"
               class="add-button ui-button button--secondary button--md">
                <i class="fas fa-list"></i> 목록으로
            </a>
            <a href="<c:out value='${customerDetailUrl}' />"
               class="add-button secondary ui-button button--secondary button--md">
                <i class="fas fa-info-circle"></i> 상세보기
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

    <!-- 수정 폼: 목록 페이지의 카드 톤과 동일한 .form-container 사용 -->
    <div class="form-container ui-form-card">
        <form class="ui-form ui-form-layout"
              method="post"
              action="${pageContext.request.contextPath}/customers"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="update">

            <c:set var="customerFormMode" value="edit" />
            <%@ include file="/WEB-INF/includes/_customer_form_fields.jspf" %>

            <!-- 버튼 -->
            <div class="button-group ui-form-actions">
                <a href="${pageContext.request.contextPath}/customers?view=list"
                   class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                <a href="<c:out value='${customerDetailUrl}' />"
                   class="btn btn-secondary ui-button button--secondary button--md">상세보기</a>
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        data-busy-label="수정 중">수정 완료</button>
            </div>
        </form>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
