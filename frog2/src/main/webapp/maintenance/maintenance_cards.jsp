<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="정기점검 이력관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance_cards.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_cards.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="maintenance-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-clipboard-check"></i> 정기점검 이력관리</jsp:attribute>
        <jsp:attribute name="subtitle">담당자별 고객사를 선택하여 정기점검 이력을 관리하세요</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/maintenance?view=add"
               class="ui-button button--primary button--md"><i class="fas fa-plus"></i> 이력 추가</a>
        </jsp:attribute>
    </t:pageHeader>
	    
    <t:flashMessages />
    
    <!-- 담당자별 고객사 카드 목록 -->
    <c:choose>
        <c:when test="${not empty inspectorCustomers}">
            <c:forEach var="entry" items="${inspectorCustomers}">
                <div class="inspector-block ui-work-surface ui-work-surface--padded">
                    <div class="inspector-section">
                        <t:sectionHeader className="inspector-header" compact="true">
                            <jsp:attribute name="title">
                                <h2 class="inspector-title ui-section-title">
                                    <i class="fas fa-user-tie"></i>
                                    <span><c:out value="${entry.key}" /></span>
                                </h2>
                            </jsp:attribute>
                        </t:sectionHeader>
                        
                        <div class="customer-grid">
                            <c:forEach var="customer" items="${entry.value}">
                                <c:url var="historyUrl" value="/maintenance">
                                    <c:param name="view" value="history" />
                                    <c:param name="customerName" value="${customer.customerName}" />
                                </c:url>
                                <a class="customer-card"
                                   href="<c:out value='${historyUrl}' />"
                                   data-detail-url="<c:out value='${historyUrl}' />">
                                    
                                    <div class="customer-name">
                                        <i class="fas fa-building"></i>
                                        <span class="customer-name-text"><c:out value="${customer.customerName}" /></span>
                                        <c:if test="${maintenanceFrequencyLabels[customer.customerName] eq '분기'}">
                                            <span class="maintenance-frequency">분기</span>
                                        </c:if>
                                    </div>
                                    
                                    <div class="customer-info">
                                        <div class="info-row">
                                            <span class="info-label">DB명</span>
                                            <span class="info-value" title="<c:out value='${customer.dbName}' />"><c:out value="${customer.dbName}" /></span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">버전</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.verticaVersion}">
                                                    <span class="version-badge ui-badge ui-badge--neutral"><c:out value="${customer.verticaVersion}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">모드</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.mode}">
                                                    <span class="mode-badge ui-badge ui-badge--neutral"><c:out value="${customer.mode}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">노드수</span>
                                            <span class="info-value"><c:out value="${customer.nodes}" /></span>
                                        </div>
                                    </div>
                                    
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="empty-state">
                <i class="fas fa-users"></i>
                <div>등록된 고객사 정보가 없습니다.</div>
                <p>먼저 고객사 정보를 등록해주세요.</p>
            </div>
        </c:otherwise>
    </c:choose>
</div>


<%@ include file="/includes/footer.jsp" %>
