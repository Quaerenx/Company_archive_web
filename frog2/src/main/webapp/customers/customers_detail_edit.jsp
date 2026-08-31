<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="고객사 상세정보 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-customer-detail-edit" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/customer_detail.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_detail_edit.js" scope="request" />
<c:set var="currentCustomerName" value="${not empty customer.customerName ? customer.customerName : customerDetail.customerName}" />

<c:url var="customerDetailUrl" value="/customers">
    <c:param name="view" value="detail" />
    <c:param name="customerName" value="${currentCustomerName}" />
    <c:param name="env" value="${env}" />
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

<%@ include file="/includes/header.jsp" %>

<div class="customer-detail customer-detail--view customer-detail--edit customer-management content-management content-shell"
     data-current-environment="<c:out value='${env}' />">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-edit" aria-hidden="true"></i>
            <c:out value="${currentCustomerName}" /> 상세정보 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">상세보기와 같은 구조에서 환경별 정보를 수정합니다.</jsp:attribute>
        <jsp:attribute name="actions">
            <div class="header-actions">
                <a href="<c:out value='${customerDetailUrl}' />"
                   class="ui-button button--secondary button--sm"
                   data-customer-environment-link>
                    <i class="fas fa-info-circle" aria-hidden="true"></i> 상세보기
                </a>
                <a href="<c:out value='${customerListReturnUrl}' />"
                   class="ui-button button--secondary button--sm">
                    <i class="fas fa-arrow-left" aria-hidden="true"></i> 목록으로
                </a>
            </div>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <div class="detail-container env-tabs ui-work-surface">
        <div class="tab-nav" role="tablist" aria-label="수정할 고객사 환경">
            <c:forEach var="environment" items="${customerDetailEnvironments}">
                <button type="button"
                        id="env-<c:out value='${environment.value}' />-edit-tab"
                        class="tab-btn ui-touch-target${env eq environment.value ? ' active' : ''}"
                        role="tab"
                        aria-selected="${env eq environment.value ? 'true' : 'false'}"
                        aria-controls="env-<c:out value='${environment.value}' />-edit"
                        tabindex="${env eq environment.value ? '0' : '-1'}"
                        data-target="env-<c:out value='${environment.value}' />-edit"
                        data-environment="<c:out value='${environment.value}' />">
                    <c:out value="${environment.label}" />
                </button>
            </c:forEach>
            <span class="tab-indicator" aria-hidden="true"></span>
        </div>

        <c:forEach var="environment" items="${customerDetailEnvironments}">
            <c:set var="editDetail" value="${environment.detail}" />
            <c:set var="editEnvironment" value="${environment.value}" />
            <c:set var="editEnvironmentLabel" value="${environment.label}" />
            <c:set var="editIdPrefix" value="${environment.value}-detail" />
            <c:url var="environmentDetailUrl" value="/customers">
                <c:param name="view" value="detail" />
                <c:param name="customerName" value="${currentCustomerName}" />
                <c:param name="env" value="${environment.value}" />
                <c:if test="${not empty param.returnFilter}"><c:param name="returnFilter" value="${param.returnFilter}" /></c:if>
                <c:if test="${not empty param.returnSortField}"><c:param name="returnSortField" value="${param.returnSortField}" /></c:if>
                <c:if test="${not empty param.returnSortDirection}"><c:param name="returnSortDirection" value="${param.returnSortDirection}" /></c:if>
                <c:if test="${not empty param.returnQ}"><c:param name="returnQ" value="${param.returnQ}" /></c:if>
                <c:if test="${not empty param.returnPage}"><c:param name="returnPage" value="${param.returnPage}" /></c:if>
                <c:if test="${not empty param.returnPageSize}"><c:param name="returnPageSize" value="${param.returnPageSize}" /></c:if>
            </c:url>

            <div class="tab-panel${env eq environment.value ? ' active' : ''}"
                 id="env-<c:out value='${environment.value}' />-edit"
                 role="tabpanel"
                 aria-labelledby="env-<c:out value='${environment.value}' />-edit-tab"
                 <c:if test="${env ne environment.value}">hidden</c:if>>
                <form id="customerDetailForm-<c:out value='${environment.value}' />"
                      class="ui-form customer-detail-environment-form"
                      method="post"
                      action="${pageContext.request.contextPath}/customers"
                      data-customer-detail-form
                      data-environment="<c:out value='${environment.value}' />"
                      data-environment-label="<c:out value='${environment.label}' />"
                      data-ui-submit-lock="auto">
                    <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                    <input type="hidden" name="action" value="saveDetail">
                    <input type="hidden" name="env" value="<c:out value='${environment.value}' />">

                    <div class="ui-alert ui-alert--danger customer-detail-error-summary"
                         data-customer-detail-error-summary
                         role="alert"
                         aria-atomic="true"
                         tabindex="-1"
                         hidden>
                        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
                        <strong>저장할 수 없습니다.</strong>
                        <span data-customer-detail-error-message>입력 내용을 다시 확인해 주세요.</span>
                    </div>

                    <div class="environment-detail ui-detail">
                        <%@ include file="/customers/_detail_edit_summary.jspf" %>
                        <%@ include file="/customers/_detail_edit_meta.jspf" %>
                        <%@ include file="/customers/_detail_edit_vertica.jspf" %>
                        <%@ include file="/customers/_detail_edit_environment.jspf" %>
                        <%@ include file="/customers/_detail_edit_solutions.jspf" %>
                        <%@ include file="/customers/_detail_edit_other.jspf" %>
                    </div>

                    <div class="form-actions ui-form-actions customer-detail-form-actions">
                        <span class="customer-detail-save-context text-muted">
                            <strong><c:out value="${environment.label}" /></strong> 환경을 저장합니다.
                        </span>
                        <button type="submit"
                                class="ui-button button--primary button--md"
                                data-busy-label="저장 중">
                            <i class="fas fa-save" aria-hidden="true"></i>
                            저장하기
                        </button>
                        <a href="<c:out value='${environmentDetailUrl}' />"
                           class="ui-button button--secondary button--md">
                            취소
                        </a>
                    </div>
                </form>
            </div>
        </c:forEach>
    </div>
</div>

<%@ include file="/includes/footer.jsp" %>
