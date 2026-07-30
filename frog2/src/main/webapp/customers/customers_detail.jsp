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
</c:url>


<div class="customer-detail customer-management content-shell" data-context-path="<c:out value='${pageContext.request.contextPath}' />">
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
				<a href="${pageContext.request.contextPath}/customers?view=list"
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

    <c:if test="${not empty sessionScope.message}">
        <div class="alert alert-success ui-alert ui-alert--success"
             role="status"
             aria-live="polite"
             aria-atomic="true">
            <i class="fas fa-check-circle"></i>
            <c:out value="${sessionScope.message}" />
        </div>
        <c:remove var="message" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-danger ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i>
            <c:out value="${sessionScope.error}" />
        </div>
        <c:remove var="error" scope="session" />
    </c:if>

    <c:set var="hasAnyDetail" value="${not empty customerDetail or not empty customerDetailStg or not empty customerDetailDev}" />
    <c:if test="${hasAnyDetail}">
        <div class="detail-container env-tabs">
            <div class="tab-nav">
                <button type="button" class="tab-btn ui-touch-target" data-target="env-prod">운영</button>
                <button type="button" class="tab-btn ui-touch-target" data-target="env-stg">스테이징</button>
                <button type="button" class="tab-btn ui-touch-target" data-target="env-dev">개발</button>
            </div>
            <div class="tab-panel" id="env-prod">
                <c:if test="${empty customerDetail}">
                    <div class="alert alert-light ui-alert ui-alert--neutral">운영 환경 데이터가 없습니다.</div>
                </c:if>
                <c:if test="${not empty customerDetail}">
                    <c:set var="detail" value="${customerDetail}" />
                    <%@ include file="/customers/_detail_sections.jspf" %>
                </c:if>
            </div>
            <div class="tab-panel" id="env-stg">
        <c:if test="${empty customerDetailStg}">
            <div class="alert alert-light ui-alert ui-alert--neutral">스테이징 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailStg}">
            <c:set var="detail" value="${customerDetailStg}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    <div class="tab-panel" id="env-dev">
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
                    <a href="${pageContext.request.contextPath}/customers?view=list"
                       class="btn btn-secondary ui-button button--secondary button--md">
                        <i class="fas fa-arrow-left"></i>
                        목록으로 돌아가기
                    </a>
				    <a href="<c:out value='${currentCustomerEditUrl}' />"
                       class="btn btn-primary ui-button button--primary button--md">
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
                <a href="${pageContext.request.contextPath}/customers?view=list"
                   class="btn btn-primary ui-button button--primary button--md">
                    <i class="fas fa-arrow-left"></i>
                    목록으로 돌아가기
                </a>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty customer}">
        <div class="detail-actions">
            <a href="#" id="editCustomerButton" data-customer-name="<c:out value='${currentCustomerName}' />"
               class="btn-min primary ui-button button--primary button--sm">
                <i class="fas fa-edit"></i> 정보수정
            </a>
            <a href="#" id="deleteCustomerButton" data-customer-name="<c:out value='${currentCustomerName}' />"
               class="btn-min danger ui-button button--danger button--sm">
                <i class="fas fa-trash"></i> 고객사 삭제
            </a>
        </div>
    </c:if>
</div>

<%@ include file="/includes/footer.jsp" %>
