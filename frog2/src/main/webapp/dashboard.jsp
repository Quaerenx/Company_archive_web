<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="대시보드" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 dashboard-page" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/dashboard.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/dashboard.js" scope="request" />
<c:set var="maintenanceDataLoaded" value="${requestScope.monthlyMaintenanceAssigneeGroups ne null}" />

<%@ include file="/includes/header.jsp" %>

<div class="dashboard-workspace content-shell">
  <t:pageHeader>
    <jsp:attribute name="title"><i class="fas fa-th-large" aria-hidden="true"></i> 대시보드</jsp:attribute>
    <jsp:attribute name="subtitle">
      <span class="dashboard-greeting-user">
        안녕하세요, <c:out value="${sessionScope.user != null ? sessionScope.user.userName : ''}"/> 님.
      </span>
      확인이 필요한 업무부터 살펴보세요.
    </jsp:attribute>
  </t:pageHeader>

  <section class="maintenance-month-board"
           id="maintenanceMonthBoard"
           aria-labelledby="maintenanceMonthTitle">
    <div class="maintenance-month-header">
      <div class="maintenance-month-title">
        <h2 id="maintenanceMonthTitle">
          <span>정기점검</span>
          <c:if test="${not empty maintenanceMonthLabel}">
            <span class="maintenance-month-label">
              <c:out value="${maintenanceMonthLabel}" />
            </span>
          </c:if>
        </h2>
      </div>
      <div class="maintenance-month-actions">
        <div class="maintenance-month-tabs" aria-label="점검 월 선택">
          <c:forEach var="monthTab" items="${maintenanceMonthTabs}">
            <c:url value="/dashboard" var="monthTabUrl">
              <c:param name="maintenanceMonth" value="${monthTab.value}" />
            </c:url>
            <c:choose>
              <c:when test="${monthTab.active}">
                <a class="maintenance-month-tab active"
                   href="${monthTabUrl}"
                   aria-current="page">
                  <c:out value="${monthTab.label}" />
                </a>
              </c:when>
              <c:otherwise>
                <a class="maintenance-month-tab" href="${monthTabUrl}">
                  <c:out value="${monthTab.label}" />
                </a>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </div>
        <button type="button"
                class="ui-button button--secondary button--sm maintenance-toggle-btn"
                id="toggleMaintenanceBoardBtn"
                aria-controls="maintenanceMonthBoardBody"
                aria-expanded="true">
          접기
        </button>
      </div>
    </div>

    <div class="maintenance-month-body"
         id="maintenanceMonthBoardBody"
         aria-busy="false">
      <div class="dashboard-state dashboard-state--loading"
           id="maintenanceLoadingState"
           role="status"
           aria-live="polite"
           hidden>
        점검 현황을 불러오는 중입니다.
      </div>

      <c:choose>
        <c:when test="${not maintenanceDataLoaded}">
          <div class="dashboard-state dashboard-state--error" role="alert">
            <strong>정기점검 현황을 불러오지 못했습니다.</strong>
            <span>대시보드를 새로 열어 다시 시도해 주세요.</span>
            <a class="ui-button button--secondary button--sm"
               href="${pageContext.request.contextPath}/dashboard">다시 열기</a>
          </div>
        </c:when>
        <c:when test="${empty monthlyMaintenanceAssigneeGroups}">
          <div class="dashboard-state dashboard-state--empty">
            <strong>이 달의 정기점검 대상 고객사가 없습니다.</strong>
            <span>전체 이력에서 다른 기간의 점검 기록을 확인할 수 있습니다.</span>
            <a class="ui-button button--secondary button--sm"
               href="${pageContext.request.contextPath}/maintenance">정기점검 이력 보기</a>
          </div>
        </c:when>
        <c:otherwise>
          <ul class="maintenance-assignee-grid"
              id="maintenanceRecordGrid"
              aria-label="담당자별 월간 정기점검 진행 현황">
            <c:forEach var="group" items="${monthlyMaintenanceAssigneeGroups}">
              <li class="maintenance-assignee-group">
                <h3 class="maintenance-assignee-name">
                  <c:out value="${group.managerName}" />
                </h3>
                <ul class="maintenance-assignee-customers">
                  <c:forEach var="customer" items="${group.customers}">
                    <c:url value="/maintenance" var="customerHistoryUrl">
                      <c:param name="view" value="history" />
                      <c:param name="customerName" value="${customer.customerName}" />
                    </c:url>
                    <li class="maintenance-assignee-customer maintenance-assignee-customer--${customer.statusCode}"
                        data-maintenance-status="${customer.statusCode}">
                      <a href="${customerHistoryUrl}">
                        <span class="maintenance-assignee-customer-name">
                          <c:out value="${customer.customerName}" />
                        </span>
                        <c:if test="${customer.quarterly}">
                          <span class="maintenance-assignee-frequency" aria-label="분기 점검">분기</span>
                        </c:if>
                        <span class="sr-only">
                          , <c:out value="${customer.statusLabel}" />
                        </span>
                      </a>
                    </li>
                  </c:forEach>
                </ul>
              </li>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </div>
  </section>

</div>

<%@ include file="/includes/footer.jsp" %>
