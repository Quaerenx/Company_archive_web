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
            <c:url var="maintenanceAddUrl" value="/maintenance">
                <c:param name="view" value="add" />
                <c:param name="returnCardsMonth" value="${maintenanceMonthParam}" />
                <c:param name="returnCardsStatus" value="${registrationStatus}" />
            </c:url>
            <a href="<c:out value='${maintenanceAddUrl}' />"
               class="ui-button button--primary button--md"><i class="fas fa-plus"></i> 이력 추가</a>
        </jsp:attribute>
    </t:pageHeader>
	    
    <t:flashMessages />

    <div class="maintenance-card-toolbar ui-work-surface ui-work-surface--padded">
        <c:url var="maintenanceCardsUrl" value="/maintenance" />
        <form class="maintenance-month-form ui-form ui-form--compact"
              method="get"
              action="<c:out value='${maintenanceCardsUrl}' />"
              data-ui-submit-lock="auto">
            <input type="hidden" name="view" value="cards" />
            <input type="hidden" name="registrationStatus" value="<c:out value='${registrationStatus}' />" />
            <label for="maintenanceMonth">점검 월</label>
            <input type="month"
                   id="maintenanceMonth"
                   name="maintenanceMonth"
                   min="1900-01"
                   max="2100-12"
                   value="<c:out value='${maintenanceMonthParam}' />"
                   aria-describedby="maintenanceRegistrationNote"
                   required />
            <button type="submit" class="ui-button button--secondary button--sm">조회</button>
        </form>
        <nav class="maintenance-registration-filters" aria-label="선택 월 이력 등록 상태">
            <c:forTokens items="all,registered,unregistered" delims="," var="statusOption">
                <c:url var="maintenanceStatusUrl" value="/maintenance">
                    <c:param name="view" value="cards" />
                    <c:param name="maintenanceMonth" value="${maintenanceMonthParam}" />
                    <c:param name="registrationStatus" value="${statusOption}" />
                </c:url>
                <a class="maintenance-registration-filter ui-button button--secondary button--sm"
                   href="<c:out value='${maintenanceStatusUrl}' />"
                   aria-current="${registrationStatus eq statusOption ? 'page' : 'false'}">
                    <c:choose>
                        <c:when test="${statusOption eq 'registered'}">등록</c:when>
                        <c:when test="${statusOption eq 'unregistered'}">미등록</c:when>
                        <c:otherwise>전체</c:otherwise>
                    </c:choose>
                </a>
            </c:forTokens>
        </nav>
        <p class="maintenance-registration-note" id="maintenanceRegistrationNote">선택 월의 이력 등록 여부를 확인하세요.</p>
    </div>
    
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
                                                       due="${maintenanceDueCustomers[customer.customerName]}"
                                                       monthLabel="${maintenanceMonthLabel}"
                                                       registrationFilter="${registrationStatus}"
                                                       frequencyLabel="${maintenanceFrequencyLabels[customer.customerName]}" />
                        </c:forEach>
                    </div>
                </c:when>
                <c:when test="${personalMaintenanceAssignedCount eq 0}">
                    <div class="personal-maintenance-customers-empty ui-empty-state">
                        <strong>담당 고객사가 없습니다.</strong>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="personal-maintenance-customers-empty ui-empty-state">
                        <strong>선택한 조건에 해당하는 담당 고객사가 없습니다.</strong>
                        <span>다른 월이나 등록 상태를 선택해 주세요.</span>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    <!-- 담당자별 고객사 카드 목록 -->
    <c:if test="${globalMaintenanceCustomerCount gt 0 or personalMaintenanceAssignedCount eq 0}">
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
                                                           due="${maintenanceDueCustomers[customer.customerName]}"
                                                           monthLabel="${maintenanceMonthLabel}"
                                                           registrationFilter="${registrationStatus}"
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
                <c:choose>
                    <c:when test="${globalMaintenanceCustomerCount gt 0}">
                        <strong>선택한 조건에 해당하는 고객사가 없습니다.</strong>
                        <span>다른 월이나 등록 상태를 선택해 주세요.</span>
                    </c:when>
                    <c:otherwise>
                        <strong>등록된 고객사 정보가 없습니다.</strong>
                        <span>먼저 고객사 정보를 등록해 주세요.</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:otherwise>
    </c:choose>
    </div>
    </c:if>
</div>


<%@ include file="/includes/footer.jsp" %>
