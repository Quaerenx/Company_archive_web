<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="대시보드" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 dashboard-page" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/dashboard.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/dashboard.js" scope="request" />
<c:set var="maintenanceDataLoaded" value="${requestScope.monthlyMaintenanceCards ne null}" />
<c:set var="vmHostModalDescriptionIds" value="vmHostModalDescription" />
<c:if test="${not empty vmHostErrorMessage}">
  <c:set var="vmHostModalDescriptionIds" value="vmHostModalDescription vmHostModalError" />
</c:if>

<%@ include file="/includes/header.jsp" %>

<main class="dashboard-workspace content-shell">
  <t:pageHeader>
    <jsp:attribute name="title"><i class="fas fa-th-large" aria-hidden="true"></i> 대시보드</jsp:attribute>
    <jsp:attribute name="subtitle">
      <span class="dashboard-greeting-user">
        안녕하세요, <c:out value="${sessionScope.user != null ? sessionScope.user.userName : ''}"/> 님.
      </span>
      확인이 필요한 업무부터 살펴보세요.
    </jsp:attribute>
  </t:pageHeader>

  <section class="maintenance-kpi-section" aria-labelledby="maintenanceKpiTitle">
    <div class="dashboard-section-heading">
      <div>
        <p class="dashboard-section-eyebrow">업무 우선순위</p>
        <h2 id="maintenanceKpiTitle">월간 정기점검 요약</h2>
      </div>
      <p class="dashboard-section-description">
        확인 필요는 예정 항목과 라이선스 위험 항목을 중복 없이 합산합니다.
      </p>
    </div>

    <div class="maintenance-kpi-grid">
      <a class="maintenance-kpi-link maintenance-kpi-link--attention"
         data-status="attention"
         href="#maintenanceMonthBoard">
        <span class="maintenance-kpi-label">확인 필요</span>
        <strong class="maintenance-kpi-value">
          <c:out value="${monthlyMaintenanceAttentionCount}" default="-" />
        </strong>
        <span class="maintenance-kpi-help">예정 또는 위험 항목</span>
      </a>
      <a class="maintenance-kpi-link maintenance-kpi-link--risk"
         data-status="license-risk"
         href="#maintenanceMonthBoard">
        <span class="maintenance-kpi-label">라이선스 위험</span>
        <strong class="maintenance-kpi-value">
          <c:out value="${monthlyMaintenanceLicenseRiskCount}" default="-" />
        </strong>
        <span class="maintenance-kpi-help">사용률 90% 이상</span>
      </a>
      <a class="maintenance-kpi-link maintenance-kpi-link--due"
         data-status="due"
         href="#maintenanceMonthBoard">
        <span class="maintenance-kpi-label">예정</span>
        <strong class="maintenance-kpi-value">
          <c:out value="${monthlyMaintenanceDueCount}" default="-" />
        </strong>
        <span class="maintenance-kpi-help">점검일이 남은 항목</span>
      </a>
      <a class="maintenance-kpi-link maintenance-kpi-link--done"
         data-status="done"
         href="#maintenanceMonthBoard">
        <span class="maintenance-kpi-label">완료</span>
        <strong class="maintenance-kpi-value">
          <c:out value="${monthlyMaintenanceDoneCount}" default="-" />
        </strong>
        <span class="maintenance-kpi-help">점검일이 지난 항목</span>
      </a>
    </div>
  </section>

  <section class="maintenance-month-board"
           id="maintenanceMonthBoard"
           aria-labelledby="maintenanceMonthTitle">
    <div class="maintenance-month-header">
      <div class="maintenance-month-title">
        <i class="fas fa-clipboard-check" aria-hidden="true"></i>
        <div>
          <p class="dashboard-section-eyebrow">핵심 업무</p>
          <h2 id="maintenanceMonthTitle">
            <c:choose>
              <c:when test="${not empty maintenanceMonthLabel}">
                <c:out value="${maintenanceMonthLabel}" /> 정기점검
              </c:when>
              <c:otherwise>월간 정기점검</c:otherwise>
            </c:choose>
          </h2>
        </div>
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

      <div class="maintenance-filter-bar" id="maintenanceFilterBar" hidden>
        <p id="maintenanceFilterStatus" role="status" aria-live="polite"></p>
        <button type="button"
                class="ui-button button--secondary button--sm"
                id="resetMaintenanceFilterBtn">
          전체 보기
        </button>
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
        <c:when test="${empty monthlyMaintenanceCards}">
          <div class="dashboard-state dashboard-state--empty">
            <strong>이 달에 등록된 정기점검 이력이 없습니다.</strong>
            <span>전체 이력에서 다른 기간의 점검 기록을 확인할 수 있습니다.</span>
            <a class="ui-button button--secondary button--sm"
               href="${pageContext.request.contextPath}/maintenance">정기점검 이력 보기</a>
          </div>
        </c:when>
        <c:otherwise>
          <div class="maintenance-record-grid" id="maintenanceRecordGrid">
            <c:forEach var="record" items="${monthlyMaintenanceCards}">
              <c:url value="/maintenance" var="recordDetailUrl">
                <c:param name="view" value="history" />
                <c:param name="customerName" value="${record.customerName}" />
              </c:url>
              <article class="maintenance-record-card"
                       data-maintenance-status="${record.statusCode}"
                       data-license-risk="${record.licenseRisk}">
                <div class="maintenance-record-top">
                  <div class="maintenance-title-group">
                    <h3 class="maintenance-customer-name">
                      <c:out value="${record.customerName}" />
                    </h3>
                    <span class="maintenance-manager">
                      <i class="fas fa-user-check" aria-hidden="true"></i>
                      <c:out value="${record.inspectorName}" />
                    </span>
                  </div>
                  <div class="maintenance-record-side">
                    <span class="maintenance-card-date">
                      <c:out value="${record.inspectionDate}" />
                    </span>
                    <div class="maintenance-status-group">
                      <span class="maintenance-status-badge ${record.statusCode}">
                        <c:out value="${record.statusLabel}" />
                      </span>
                      <c:if test="${record.licenseRisk}">
                        <span class="maintenance-status-badge risk">라이선스 위험</span>
                      </c:if>
                    </div>
                  </div>
                </div>
                <dl class="maintenance-card-lines">
                  <div class="maintenance-card-line">
                    <dt>Vertica</dt>
                    <dd><c:out value="${record.verticaVersion}" /></dd>
                  </div>
                  <div class="maintenance-card-line">
                    <dt>라이선스</dt>
                    <dd><c:out value="${record.licenseSummary}" /></dd>
                  </div>
                </dl>
                <p class="maintenance-note"><c:out value="${record.note}" /></p>
                <div class="maintenance-card-actions">
                  <a class="maintenance-detail-link"
                     href="${recordDetailUrl}">
                    <span class="sr-only"><c:out value="${record.customerName}" /></span>
                    점검 이력 보기
                    <i class="fas fa-chevron-right" aria-hidden="true"></i>
                  </a>
                </div>
              </article>
            </c:forEach>
          </div>
          <div class="dashboard-state dashboard-state--empty"
               id="maintenanceFilteredEmpty"
               hidden>
            선택한 조건에 해당하는 점검 항목이 없습니다.
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </section>

  <section class="dashboard-quick-actions" aria-labelledby="dashboardQuickActionsTitle">
    <div class="dashboard-section-heading">
      <div>
        <p class="dashboard-section-eyebrow">빠른 이동</p>
        <h2 id="dashboardQuickActionsTitle">주요 업무</h2>
      </div>
    </div>
    <div class="dashboard-action-grid">
      <c:forEach var="entry" items="${dashboardMenus}">
        <article class="dashboard-action-group">
          <h3>
            <c:set var="cat" value="${entry.key}" />
            <c:choose>
              <c:when test="${cat == '고객 관리'}"><i class="fas fa-address-book" aria-hidden="true"></i></c:when>
              <c:when test="${cat == '자료 관리'}"><i class="fas fa-folder-open" aria-hidden="true"></i></c:when>
              <c:otherwise><i class="fas fa-th-large" aria-hidden="true"></i></c:otherwise>
            </c:choose>
            <c:out value="${entry.key}" />
          </h3>
          <ul class="dashboard-submenu">
            <c:forEach var="menuItem" items="${entry.value}">
              <li>
                <a href="${pageContext.request.contextPath}/<c:out value='${menuItem.url}' />">
                  <i class="<c:out value='${menuItem.icon}' /> fa-fw" aria-hidden="true"></i>
                  <span><c:out value="${menuItem.title}" /></span>
                  <i class="fas fa-chevron-right" aria-hidden="true"></i>
                </a>
              </li>
            </c:forEach>
          </ul>
        </article>
      </c:forEach>
    </div>
  </section>

  <section class="card dashboard-card vm-board" aria-labelledby="vmHostBoardTitle">
    <div class="card-header vm-board-header">
      <div class="vm-board-title">
        <i class="fas fa-network-wired" aria-hidden="true"></i>
        <div>
          <h2 id="vmHostBoardTitle">개인 호스트 관리</h2>
          <span class="vm-board-caption">
            <c:out value="${vmHostCount}" default="0" /> / <c:out value="${vmHostLimit}" default="20" />개 사용
          </span>
        </div>
      </div>
      <div class="vm-board-actions">
        <button type="button"
                class="ui-button button--secondary button--sm vm-toggle-btn"
                id="toggleVmHostBoardBtn"
                aria-controls="vmHostBoardBody"
                aria-expanded="true">
          접기
        </button>
        <button type="button"
                class="ui-button button--secondary button--sm vm-add-btn"
                id="openVmHostAddBtn">
          호스트 등록
        </button>
      </div>
    </div>
    <div class="card-body vm-board-body" id="vmHostBoardBody">
      <c:if test="${param.vmHostResult == 'saved'}">
        <div class="vm-message ui-alert ui-alert--success" role="status">
          호스트 정보가 저장되었습니다.
        </div>
      </c:if>
      <c:if test="${param.vmHostResult == 'deleted'}">
        <div class="vm-message ui-alert ui-alert--success" role="status">
          호스트가 삭제되었습니다.
        </div>
      </c:if>
      <p class="vm-board-note">개인 개발·검증용 VM 연결 정보를 관리합니다.</p>

      <div class="vm-table-wrap ui-table-wrap">
        <table class="vm-table ui-table">
          <caption class="sr-only">개인 VM 호스트 목록</caption>
          <thead>
            <tr>
              <th scope="col">사용 호스트</th>
              <th scope="col">목적</th>
              <th scope="col">OS</th>
              <th scope="col">VERTICA-ver</th>
              <th scope="col">원격지</th>
              <th scope="col">비고</th>
              <th scope="col">관리</th>
            </tr>
          </thead>
          <tbody>
            <c:choose>
              <c:when test="${empty vmHosts}">
                <tr>
                  <td colspan="7">등록된 VM 호스트가 없습니다.</td>
                </tr>
              </c:when>
              <c:otherwise>
                <c:forEach var="host" items="${vmHosts}">
                  <tr>
                    <td><strong><c:out value="${host.ip}"/></strong></td>
                    <td><c:out value="${host.purpose}"/></td>
                    <td><c:out value="${host.osInfo}"/></td>
                    <td><c:out value="${host.verticaVersion}"/></td>
                    <td><c:out value="${host.remoteHost}"/></td>
                    <td><c:out value="${host.note}"/></td>
                    <td>
                      <button type="button"
                              class="ui-button button--secondary button--sm vm-edit-btn"
                              data-ip="<c:out value='${host.ip}'/>"
                              data-purpose="<c:out value='${host.purpose}'/>"
                              data-os-info="<c:out value='${host.osInfo}'/>"
                              data-vertica-version="<c:out value='${host.verticaVersion}'/>"
                              data-remote-host="<c:out value='${host.remoteHost}'/>"
                              data-note="<c:out value='${host.note}'/>">
                        <span class="sr-only"><c:out value="${host.ip}" /></span>
                        수정
                      </button>
                    </td>
                  </tr>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <details class="dashboard-support">
    <summary>외부 참고 링크</summary>
    <ul class="dashboard-support-links">
      <li>
        <a href="https://docs.vertica.com/" target="_blank" rel="noopener noreferrer">
          Vertica 공식 문서 <span class="sr-only">(새 창)</span>
        </a>
      </li>
      <li>
        <a href="https://x2wizard.github.io/" target="_blank" rel="noopener noreferrer">
          Vertica 블로그 <span class="sr-only">(새 창)</span>
        </a>
      </li>
      <li>
        <a href="https://www.microfocus.com/lifecycle/" target="_blank" rel="noopener noreferrer">
          Vertica EOS 정보 <span class="sr-only">(새 창)</span>
        </a>
      </li>
      <li>
        <a href="https://portal.microfocus.com/s/customdetailpage" target="_blank" rel="noopener noreferrer">
          Vertica Case Open <span class="sr-only">(새 창)</span>
        </a>
      </li>
    </ul>
  </details>
</main>

<c:if test="${not empty vmHostForm}">
  <div id="vmHostFormSeed" hidden
       data-ip="<c:out value='${vmHostForm.ip}'/>"
       data-original-ip="<c:out value='${vmHostOriginalIp}'/>"
       data-purpose="<c:out value='${vmHostForm.purpose}'/>"
       data-os-info="<c:out value='${vmHostForm.osInfo}'/>"
       data-vertica-version="<c:out value='${vmHostForm.verticaVersion}'/>"
       data-remote-host="<c:out value='${vmHostForm.remoteHost}'/>"
       data-note="<c:out value='${vmHostForm.note}'/>"></div>
</c:if>

<div class="vm-modal-backdrop"
     id="vmHostModalBackdrop"
     aria-hidden="true"
     hidden>
  <div class="vm-modal"
       id="vmHostModal"
       role="dialog"
       aria-modal="true"
       aria-labelledby="vmHostModalTitle"
       aria-describedby="${vmHostModalDescriptionIds}"
       tabindex="-1">
    <div class="vm-modal-header">
      <div>
        <h2 id="vmHostModalTitle">호스트 등록</h2>
        <p id="vmHostModalDescription">개인 개발·검증용 호스트 정보를 입력합니다.</p>
      </div>
      <button type="button"
              class="ui-button button--secondary button--sm vm-modal-close"
              id="closeVmHostModalBtn"
              aria-label="호스트 관리 창 닫기">
        ×
      </button>
    </div>
    <div class="vm-modal-body">
      <c:if test="${not empty vmHostErrorMessage}">
        <div id="vmHostModalError"
             class="vm-error ui-alert ui-alert--danger"
             role="alert">
          <c:out value="${vmHostErrorMessage}"/>
        </div>
      </c:if>
      <form class="vm-form ui-form"
            id="vmHostSaveForm"
            method="post"
            data-ui-submit-lock="auto"
            action="${pageContext.request.contextPath}/vm-hosts">
        <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
        <input type="hidden" name="action" value="save" />
        <input type="hidden" name="returnTo" value="dashboard" />
        <input type="hidden" name="originalIp" id="vmHostOriginalIp" />

        <div class="ui-field">
          <label for="vmHostIp">사용 호스트</label>
          <input type="text"
                 name="ip"
                 id="vmHostIp"
                 maxlength="15"
                 placeholder="192.168.40.60"
                 required />
        </div>

        <div class="ui-field">
          <label for="vmHostPurpose">목적</label>
          <input type="text"
                 name="purpose"
                 id="vmHostPurpose"
                 maxlength="500"
                 placeholder="예: 개인 개발 VM"
                 required />
        </div>

        <div class="vm-form-grid">
          <div class="ui-field">
            <label for="vmHostOsInfo">OS</label>
            <input type="text"
                   name="osInfo"
                   id="vmHostOsInfo"
                   maxlength="100"
                   placeholder="예: Ubuntu 22.04.5" />
          </div>
          <div class="ui-field">
            <label for="vmHostVerticaVersion">VERTICA-ver</label>
            <input type="text"
                   name="verticaVersion"
                   id="vmHostVerticaVersion"
                   maxlength="50"
                   placeholder="예: 24.3.0-3" />
          </div>
        </div>

        <div class="ui-field">
          <label for="vmHostRemoteHost">원격지</label>
          <input type="text"
                 name="remoteHost"
                 id="vmHostRemoteHost"
                 maxlength="100"
                 placeholder="예: 192.168.40.160" />
        </div>

        <div class="ui-field">
          <label for="vmHostNote">비고</label>
          <textarea name="note"
                    id="vmHostNote"
                    placeholder="메모, 계정, 용도 등을 기록하세요."></textarea>
        </div>
      </form>
    </div>
    <div class="vm-modal-footer">
      <div class="vm-modal-footer-left">
        <form id="vmHostDeleteForm" method="post"
              class="ui-form"
              data-ui-submit-lock="auto"
              action="${pageContext.request.contextPath}/vm-hosts"
              hidden>
          <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
          <input type="hidden" name="action" value="delete" />
          <input type="hidden" name="returnTo" value="dashboard" />
          <input type="hidden" name="ip" id="vmHostDeleteIp" />
          <button type="submit"
                  class="ui-button button--danger button--md"
                  data-busy-label="삭제 중">
            삭제
          </button>
        </form>
      </div>
      <div class="vm-modal-footer-right">
        <button type="button"
                class="ui-button button--secondary button--md"
                id="cancelVmHostModalBtn">
          취소
        </button>
        <button type="submit"
                class="ui-button button--primary button--md"
                form="vmHostSaveForm"
                data-busy-label="저장 중">
          저장
        </button>
      </div>
    </div>
  </div>
</div>

<%@ include file="/includes/footer.jsp" %>
