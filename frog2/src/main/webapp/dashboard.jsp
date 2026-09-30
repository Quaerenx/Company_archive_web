<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="대시보드" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 dashboard-page" scope="request" />
<%-- 두 문서가 opt-in하므로 parser-blocking route gate가 실제 로그인/로그아웃
     경로만 남기고 오류 응답과 대시보드 내부 이동은 즉시 전환한다. --%>
<c:set var="pageCss" value="/resources/css/view-transitions.css,/resources/css/pages/dashboard.css" scope="request" />
<c:set var="pageHeadScript" value="/resources/js/view-transition-routing.js" scope="request" />
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
    <jsp:attribute name="extra">
      <div class="maintenance-month-selector">
        <span class="maintenance-month-selector-label">점검 월</span>
        <nav class="maintenance-month-tabs" aria-label="점검 월 선택"
             aria-describedby="dashboardMaintenanceMonthScope">
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
        </nav>
        <span id="dashboardMaintenanceMonthScope" class="maintenance-month-scope">나의·전체 점검에 적용</span>
      </div>
    </jsp:attribute>
  </t:pageHeader>

  <section class="maintenance-month-board ui-work-surface ui-work-surface--padded personal-maintenance-board"
           data-personal-section="dashboard-maintenance"
           aria-busy="false"
           aria-labelledby="personalMaintenanceTitle">
    <t:sectionHeader className="maintenance-month-header" flush="true">
      <jsp:attribute name="title">
        <div class="maintenance-month-title">
          <h2 id="personalMaintenanceTitle" class="ui-section-title">
            <span>나의 정기점검</span>
            <c:if test="${not empty maintenanceMonthLabel}">
              <span class="maintenance-month-label"><c:out value="${maintenanceMonthLabel}" /></span>
            </c:if>
          </h2>
        </div>
      </jsp:attribute>
    </t:sectionHeader>
    <div class="ui-section-body ui-section-body--flush">
      <c:if test="${maintenanceDataLoaded and personalMaintenanceAssignedCount gt 0}">
        <c:url var="personalMaintenanceManageUrl" value="/maintenance">
          <c:param name="view" value="cards" />
          <c:param name="maintenanceMonth" value="${maintenanceMonthParam}" />
        </c:url>
        <div class="personal-maintenance-summary">
          <dl class="personal-maintenance-summary__counts" aria-label="선택 월 나의 정기점검 이력 등록 현황">
            <div>
              <dt>대상 고객사</dt>
              <dd><c:out value="${personalMaintenanceTargetCount}" /><span>개</span></dd>
            </div>
            <div class="personal-maintenance-summary__registered">
              <dt>이력 등록</dt>
              <dd><c:out value="${personalMaintenanceRegisteredCount}" /><span>개</span></dd>
            </div>
            <div>
              <dt>이력 미등록</dt>
              <dd><c:out value="${personalMaintenanceUnregisteredCount}" /><span>개</span></dd>
            </div>
          </dl>
          <div class="personal-maintenance-actions">
            <c:if test="${personalMaintenanceUnregisteredCount gt 0}">
              <c:url var="personalMaintenanceUnregisteredUrl" value="/maintenance">
                <c:param name="view" value="cards" />
                <c:param name="maintenanceMonth" value="${maintenanceMonthParam}" />
                <c:param name="registrationStatus" value="unregistered" />
              </c:url>
              <a class="ui-button button--primary button--sm personal-maintenance-unregistered-link"
                 href="<c:out value='${personalMaintenanceUnregisteredUrl}' />#personalMaintenanceCustomersTitle">미등록 점검 확인 <i class="fas fa-chevron-right" aria-hidden="true"></i></a>
            </c:if>
            <a class="ui-button button--secondary button--sm personal-maintenance-manage-link"
               href="<c:out value='${personalMaintenanceManageUrl}' />">선택 월 점검 관리 <i class="fas fa-chevron-right" aria-hidden="true"></i></a>
          </div>
        </div>
      </c:if>
      <c:choose>
        <c:when test="${not maintenanceDataLoaded}">
          <div class="dashboard-state dashboard-state--error" role="alert">
            <strong>정기점검 현황을 불러오지 못했습니다.</strong>
          </div>
        </c:when>
        <c:when test="${personalMaintenanceAssignedCount eq 0}">
          <div class="dashboard-state dashboard-state--empty personal-maintenance-empty">
            <strong>담당 고객사가 없습니다.</strong>
          </div>
        </c:when>
        <c:when test="${empty personalMaintenanceCustomers}">
          <div class="dashboard-state dashboard-state--empty personal-maintenance-empty">
            <strong>선택한 달의 정기점검 대상 고객사가 없습니다.</strong>
          </div>
        </c:when>
        <c:otherwise>
          <ul class="maintenance-status-legend" aria-label="정기점검 상태 범례">
            <li class="maintenance-status-legend__item maintenance-status-legend__item--done">완료</li>
            <li class="maintenance-status-legend__item maintenance-status-legend__item--due">미진행</li>
            <li class="maintenance-status-legend__frequency">분기</li>
          </ul>
          <t:monthlyMaintenanceCustomerList customers="${personalMaintenanceCustomers}" />
        </c:otherwise>
      </c:choose>
    </div>
  </section>

  <section class="maintenance-month-board ui-work-surface ui-work-surface--padded"
           id="maintenanceMonthBoard"
           data-global-section="dashboard-maintenance"
           aria-labelledby="maintenanceMonthTitle">
    <div class="maintenance-month-header ui-section-header ui-section-header--flush">
      <div class="maintenance-month-title">
        <h2 id="maintenanceMonthTitle" class="ui-section-title">
          <span>정기점검</span>
          <c:if test="${not empty maintenanceMonthLabel}">
            <span class="maintenance-month-label">
              <c:out value="${maintenanceMonthLabel}" />
            </span>
          </c:if>
        </h2>
      </div>
      <div class="maintenance-month-actions">
        <button type="button"
                class="ui-button button--secondary button--sm maintenance-toggle-btn"
                id="toggleMaintenanceBoardBtn"
                aria-controls="maintenanceMonthBoardBody"
                aria-expanded="true">
          접기
        </button>
      </div>
    </div>

    <div class="maintenance-month-body ui-section-body ui-section-body--flush"
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
          <ul class="maintenance-status-legend" aria-label="정기점검 상태 범례">
            <li class="maintenance-status-legend__item maintenance-status-legend__item--done">완료</li>
            <li class="maintenance-status-legend__item maintenance-status-legend__item--due">미진행</li>
            <li class="maintenance-status-legend__frequency">분기</li>
          </ul>
          <ul class="maintenance-assignee-grid"
              id="maintenanceRecordGrid"
              aria-label="담당자별 월간 정기점검 진행 현황">
            <c:forEach var="group" items="${monthlyMaintenanceAssigneeGroups}">
              <li class="maintenance-assignee-group ui-subgroup">
                <h3 class="maintenance-assignee-name">
                  <c:out value="${group.managerName}" />
                </h3>
                <t:monthlyMaintenanceCustomerList customers="${group.customers}" />
              </li>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </div>
  </section>

</div>

<%@ include file="/includes/footer.jsp" %>
