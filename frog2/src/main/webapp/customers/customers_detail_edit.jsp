<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="고객사 상세정보 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-customer-detail-edit" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_detail_edit.js" scope="request" />
<c:set var="currentCustomerName" value="${not empty customerDetail.customerName ? customerDetail.customerName : customer.customerName}" />
<c:url var="customerDetailUrl" value="/customers">
    <c:param name="view" value="detail" />
    <c:param name="customerName" value="${currentCustomerName}" />
    <c:if test="${not empty param.returnFilter}"><c:param name="returnFilter" value="${param.returnFilter}" /></c:if>
    <c:if test="${not empty param.returnSortField}"><c:param name="returnSortField" value="${param.returnSortField}" /></c:if>
    <c:if test="${not empty param.returnSortDirection}"><c:param name="returnSortDirection" value="${param.returnSortDirection}" /></c:if>
    <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
</c:url>
<c:url var="customerListReturnUrl" value="/customers">
    <c:param name="view" value="list" />
    <c:if test="${not empty param.returnFilter}"><c:param name="filter" value="${param.returnFilter}" /></c:if>
    <c:if test="${not empty param.returnSortField}"><c:param name="sortField" value="${param.returnSortField}" /></c:if>
    <c:if test="${not empty param.returnSortDirection}"><c:param name="sortDirection" value="${param.returnSortDirection}" /></c:if>
    <c:if test="${not empty param.returnQ}"><c:param name="q" value="${param.returnQ}" /></c:if>
    <c:if test="${not empty param.returnPage}"><c:param name="page" value="${param.returnPage}" /></c:if>
    <c:if test="${not empty param.returnPageSize}"><c:param name="pageSize" value="${param.returnPageSize}" /></c:if>
</c:url>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="customer-detail customer-management content-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-edit"></i>
            <c:choose>
                <c:when test="${not empty customerDetail.customerName}"><c:out value="${customerDetail.customerName}" /></c:when>
                <c:when test="${not empty customer.customerName}"><c:out value="${customer.customerName}" /></c:when>
                <c:otherwise>고객사</c:otherwise>
            </c:choose>
            상세정보 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">고객사 상세정보를 수정하세요</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${customerDetailUrl}' />"
               class="ui-button button--secondary button--md">
                <i class="fas fa-info-circle"></i> 상세보기
            </a>
            <a href="<c:out value='${customerListReturnUrl}' />"
               class="ui-button button--secondary button--md">
                <i class="fas fa-list"></i> 목록으로
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <form id="customerDetailForm"
          class="ui-form"
          method="post"
          action="${pageContext.request.contextPath}/customers"
          data-ui-submit-lock="auto">
        <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
        <input type="hidden" name="action" value="saveDetail">
        <input type="hidden" name="env" value="<c:out value="${env != null ? env : 'prod'}" />">

        <nav class="customer-detail-edit-nav"
             aria-label="상세정보 수정 섹션">
            <a href="#customerDetailMeta">기본·담당자</a>
            <a href="#customerDetailVertica">Vertica</a>
            <a href="#customerDetailEnvironment">환경·네트워크</a>
            <a href="#customerDetailSolutions">외부 솔루션</a>
            <a href="#customerDetailOther">기타</a>
        </nav>

        <div id="customerDetailErrorSummary"
             class="ui-alert ui-alert--danger customer-detail-error-summary"
             role="alert"
             aria-atomic="true"
             tabindex="-1"
             hidden>
            <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
            <strong>저장할 수 없습니다.</strong>
            <span data-customer-detail-error-message>입력 내용을 다시 확인해 주세요.</span>
        </div>

        <div class="detail-container ui-detail ui-work-surface">
            <%@ include file="/customers/_detail_edit_meta.jspf" %>
            <%@ include file="/customers/_detail_edit_vertica.jspf" %>
            <%@ include file="/customers/_detail_edit_environment.jspf" %>
            <%@ include file="/customers/_detail_edit_solutions.jspf" %>
            <%@ include file="/customers/_detail_edit_other.jspf" %>
        </div>

        <!-- 버튼 그룹 -->
        <div class="form-actions ui-form-actions customer-detail-form-actions">
            <button type="submit"
                    class="ui-button button--primary button--md"
                    data-busy-label="저장 중">
                <i class="fas fa-save"></i>
                저장하기
            </button>
            <a href="<c:out value='${customerDetailUrl}' />"
               class="ui-button button--secondary button--md">
                <i class="fas fa-times"></i>
                취소
            </a>
            <a href="<c:out value='${customerListReturnUrl}' />"
               class="ui-button button--secondary button--md">
                <i class="fas fa-list"></i>
                목록으로
            </a>
        </div>
    </form>
</div>

<%@ include file="/includes/footer.jsp" %>
