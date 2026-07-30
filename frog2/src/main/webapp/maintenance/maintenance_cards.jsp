<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="정기점검 이력관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/maintenance_cards.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_cards.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="maintenance-management content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-clipboard-check"></i> 정기점검 이력관리</jsp:attribute>
        <jsp:attribute name="subtitle">담당자별 고객사를 선택하여 정기점검 이력을 관리하세요</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/maintenance?view=add"
               class="add-button ui-button button--primary button--md"><i class="fas fa-plus"></i> 이력 추가</a>
        </jsp:attribute>
    </t:pageHeader>
	    
	<!-- 성공/에러 메시지 표시 -->
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
    
    <!-- 담당자별 고객사 카드 목록 -->
    <c:choose>
        <c:when test="${not empty inspectorCustomers}">
            <!-- 접속 사용자와 동일한 점검자 섹션을 우선 배치 -->
            <c:forEach var="entry" items="${inspectorCustomers}">
                <c:if test="${entry.key == user.userName}">
                    <div class="inspector-block">
                    <div class="inspector-section">
                        <div class="inspector-header">
                            <i class="fas fa-user-tie"></i>
                            <span><c:out value="${entry.key}" /></span>
                        </div>
                        
                        <div class="customer-grid">
                            <c:forEach var="customer" items="${entry.value}">
                                <c:url var="historyUrl" value="/maintenance">
                                    <c:param name="view" value="history" />
                                    <c:param name="customerName" value="${customer.customerName}" />
                                </c:url>
                                <div class="customer-card"
                                     data-detail-url="<c:out value='${historyUrl}' />">
                                    
                                    <div class="customer-name">
                                        <i class="fas fa-building"></i>
                                        <c:out value="${customer.customerName}" />
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
                                                    <span class="version-badge ui-badge"><c:out value="${customer.verticaVersion}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">모드</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.mode}">
                                                    <span class="mode-badge ui-badge"><c:out value="${customer.mode}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">노드수</span>
                                            <span class="info-value"><c:out value="${customer.nodes}" /></span>
                                        </div>
                                    </div>
                                    
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    </div>
                </c:if>
            </c:forEach>

            <!-- 나머지 점검자 섹션들 -->
            <c:forEach var="entry" items="${inspectorCustomers}">
                <c:if test="${entry.key != user.userName}">
                    <div class="inspector-block">
                    <div class="inspector-section">
                        <div class="inspector-header">
                            <i class="fas fa-user-tie"></i>
                            <span><c:out value="${entry.key}" /></span>
                        </div>
                        
                        <div class="customer-grid">
                            <c:forEach var="customer" items="${entry.value}">
                                <c:url var="historyUrl" value="/maintenance">
                                    <c:param name="view" value="history" />
                                    <c:param name="customerName" value="${customer.customerName}" />
                                </c:url>
                                <div class="customer-card"
                                     data-detail-url="<c:out value='${historyUrl}' />">
                                    
                                    <div class="customer-name">
                                        <i class="fas fa-building"></i>
                                        <c:out value="${customer.customerName}" />
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
                                                    <span class="version-badge ui-badge"><c:out value="${customer.verticaVersion}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">모드</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.mode}">
                                                    <span class="mode-badge ui-badge"><c:out value="${customer.mode}" /></span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">노드수</span>
                                            <span class="info-value"><c:out value="${customer.nodes}" /></span>
                                        </div>
                                    </div>
                                    
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    </div>
                </c:if>
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
