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
    
    <section class="inspector-block ui-work-surface ui-work-surface--padded personal-maintenance-customers"
             data-personal-section="maintenance-customers"
             aria-labelledby="personalMaintenanceCustomersTitle">
        <div class="inspector-section">
            <t:sectionHeader className="inspector-header" compact="true">
                <jsp:attribute name="title">
                    <h2 id="personalMaintenanceCustomersTitle" class="inspector-title ui-section-title">
                        <i class="fas fa-user-tie" aria-hidden="true"></i>
                        <span>나의 정기점검 고객사</span>
                    </h2>
                </jsp:attribute>
            </t:sectionHeader>
            <c:choose>
                <c:when test="${not empty personalMaintenanceCustomers}">
                    <div class="customer-grid">
                        <c:forEach var="customer" items="${personalMaintenanceCustomers}">
                            <t:maintenanceCustomerCard customer="${customer}"
                                                       registered="${currentMonthMaintenanceCustomers[customer.customerName]}"
                                                       frequencyLabel="${maintenanceFrequencyLabels[customer.customerName]}" />
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="personal-maintenance-customers-empty ui-empty-state">
                        <strong>담당 고객사가 없습니다.</strong>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <!-- 담당자별 고객사 카드 목록 -->
    <div data-global-section="maintenance-customers">
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
                                <t:maintenanceCustomerCard customer="${customer}"
                                                           registered="${currentMonthMaintenanceCustomers[customer.customerName]}"
                                                           frequencyLabel="${maintenanceFrequencyLabels[customer.customerName]}" />
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="maintenance-cards-empty ui-empty-state">
                <i class="fas fa-users" aria-hidden="true"></i>
                <strong>등록된 고객사 정보가 없습니다.</strong>
                <span>먼저 고객사 정보를 등록해 주세요.</span>
            </div>
        </c:otherwise>
    </c:choose>
    </div>
</div>


<%@ include file="/includes/footer.jsp" %>
