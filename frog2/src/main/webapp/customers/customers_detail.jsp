<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="고객사 상세정보" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/customer_detail.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_detail.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>



<c:set var="currentCustomerName" value="${not empty customerDetail.customerName ? customerDetail.customerName : (not empty customer.customerName ? customer.customerName : '')}" />
<c:url var="currentCustomerEditUrl" value="/customers">
    <c:param name="view" value="editDetail" />
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


<div class="customer-detail customer-detail--view customer-management content-management content-shell" data-context-path="<c:out value='${pageContext.request.contextPath}' />">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-building"></i>
            <c:choose>
                <c:when test="${not empty customerDetail.customerName}">
                    <c:out value="${customerDetail.customerName}" />
                </c:when>
                <c:when test="${not empty customer.customerName}">
                    <c:out value="${customer.customerName}" />
                </c:when>
                <c:otherwise>
                    고객사
                </c:otherwise>
            </c:choose>
            상세정보
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            고객사 정보 및 시스템 세부사항
        </jsp:attribute>
		<jsp:attribute name="actions">
			<div class="header-actions">
					<a href="<c:out value='${customerListReturnUrl}' />"
                   class="btn-min ui-button button--secondary button--sm">
					<i class="fas fa-arrow-left"></i> 목록으로
				</a>
			</div>
		</jsp:attribute>
        <jsp:attribute name="extra">
            <c:if test="${not empty verticaEosDate}">
                <div class="alert alert-warning customer-eos-alert ui-alert ui-alert--warning"
                     role="status"
                     aria-live="polite"
                     aria-atomic="true">
                    <i class="fas fa-exclamation-triangle"></i>
                    Vertica EOS 일자: <strong><fmt:formatDate value="${verticaEosDate}" pattern="yyyy-MM-dd"/></strong>
                </div>
            </c:if>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <c:set var="hasAnyDetail" value="${not empty customerDetail or not empty customerDetailStg or not empty customerDetailDev}" />
    <c:if test="${hasAnyDetail}">
        <div class="detail-container env-tabs">
            <div class="tab-nav" role="tablist" aria-label="고객사 환경">
                <button type="button" id="env-prod-tab" class="tab-btn ui-touch-target active"
                        role="tab" aria-selected="true" aria-controls="env-prod" tabindex="0"
                        data-target="env-prod">운영</button>
                <button type="button" id="env-stg-tab" class="tab-btn ui-touch-target"
                        role="tab" aria-selected="false" aria-controls="env-stg" tabindex="-1"
                        data-target="env-stg">스테이징</button>
                <button type="button" id="env-dev-tab" class="tab-btn ui-touch-target"
                        role="tab" aria-selected="false" aria-controls="env-dev" tabindex="-1"
                        data-target="env-dev">개발</button>
            </div>
            <div class="tab-panel active" id="env-prod" role="tabpanel" aria-labelledby="env-prod-tab">
                <c:if test="${empty customerDetail}">
                    <div class="alert alert-light ui-alert ui-alert--neutral">운영 환경 데이터가 없습니다.</div>
                </c:if>
                <c:if test="${not empty customerDetail}">
                    <c:set var="detail" value="${customerDetail}" />
                    <%@ include file="/customers/_detail_sections.jspf" %>
                </c:if>
            </div>
            <div class="tab-panel" id="env-stg" role="tabpanel" aria-labelledby="env-stg-tab" hidden>
        <c:if test="${empty customerDetailStg}">
            <div class="alert alert-light ui-alert ui-alert--neutral">스테이징 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailStg}">
            <c:set var="detail" value="${customerDetailStg}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    <div class="tab-panel" id="env-dev" role="tabpanel" aria-labelledby="env-dev-tab" hidden>
        <c:if test="${empty customerDetailDev}">
            <div class="alert alert-light ui-alert ui-alert--neutral">개발 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailDev}">
            <c:set var="detail" value="${customerDetailDev}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    </div>
    </c:if>

    <c:if test="${empty customerDetail and not empty customer}">
        <div class="detail-container">
            <div class="detail-section text-center p-5">
                <i class="fas fa-info-circle text-warning customer-empty-icon"></i>
                <h3 class="text-dark mb-3">상세정보가 등록되지 않았습니다</h3>
                <p class="text-muted mb-4">
                    <c:out value="${customer.customerName}" />의 기본 정보는 있지만 상세정보가 등록되지 않았습니다.<br>
                    상세정보를 등록하려면 수정 페이지에서 추가해 주세요.
                </p>
                <div class="d-flex gap-3 justify-content-center">
                    <a href="<c:out value='${customerListReturnUrl}' />"
                       class="btn btn-secondary ui-button button--secondary button--md">
                        <i class="fas fa-arrow-left"></i>
                        목록으로 돌아가기
                    </a>
				    <a href="<c:out value='${currentCustomerEditUrl}' />"
                       class="btn btn-secondary ui-button button--secondary button--md">
				        <i class="fas fa-edit"></i>
				        정보 수정하기
				    </a>
                </div>
            </div>
        </div>
    </c:if>

    <c:if test="${empty customer and not hasAnyDetail}">
        <div class="detail-container">
            <div class="detail-section text-center p-5">
                <i class="fas fa-exclamation-triangle text-danger customer-empty-icon"></i>
                <h3 class="text-dark mb-3">고객사 정보를 찾을 수 없습니다</h3>
                <p class="text-muted mb-4">요청하신 고객사 정보가 존재하지 않거나 삭제되었을 수 있습니다.</p>
                <a href="<c:out value='${customerListReturnUrl}' />"
                   class="btn btn-secondary ui-button button--secondary button--md">
                    <i class="fas fa-arrow-left"></i>
                    목록으로 돌아가기
                </a>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty customer}">
        <div class="detail-actions">
            <a href="<c:out value='${currentCustomerEditUrl}' />"
               id="editCustomerButton" data-customer-name="<c:out value='${currentCustomerName}' />"
               class="btn-min ui-button button--secondary button--sm">
                <i class="fas fa-edit"></i> 정보수정
            </a>
            <button type="button"
                    id="deleteCustomerButton"
                    data-customer-name="<c:out value='${currentCustomerName}' />"
                    class="btn-min danger ui-button button--danger button--sm"
                    data-busy-label="삭제 중">
                <i class="fas fa-trash"></i> 고객사 삭제
            </button>
        </div>
    </c:if>
</div>

<%@ include file="/includes/footer.jsp" %>
