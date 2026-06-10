<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="대시보드" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${pageTitle}</title>
  <link rel="icon" href="${pageContext.request.contextPath}/favicon.png?v=20251017" type="image/png" sizes="32x32">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png?v=20251017">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main_style.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/components.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/utilities.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/dashboard.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/dashboard_box.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
  <style>
    .dashboard-card .card-header,
    .vm-board-header {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .dashboard-submenu a {
      display: inline-flex;
      align-items: center;
    }
    .customer-management .page-header .ph-subtitle {
      display: block !important;
    }
    .vm-board {
      margin-bottom: 24px;
    }
    .maintenance-month-board {
      margin-bottom: 24px;
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
      overflow: hidden;
    }
    .maintenance-month-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 16px 20px;
      border-bottom: 1px solid #e2e8f0;
    }
    .maintenance-month-title {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .maintenance-month-title h2 {
      margin: 0;
      font-size: 1.35rem;
      line-height: 1.25;
      letter-spacing: 0;
    }
    .maintenance-month-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 8px;
      flex: 0 0 auto;
      flex-wrap: wrap;
    }
    .maintenance-toggle-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 34px;
      padding: 0 12px;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      background: #fff;
      color: #334155;
      font-size: 0.82rem;
      font-weight: 700;
      cursor: pointer;
    }
    .maintenance-toggle-btn:hover {
      background: #f8fafc;
    }
    .maintenance-month-body.is-collapsed {
      display: none;
    }
    .maintenance-month-tabs {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      padding: 3px;
      background: #eef2f7;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
    }
    .maintenance-month-tab {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 28px;
      padding: 0 12px;
      border: 0;
      border-radius: 7px;
      background: transparent;
      color: #64748b;
      font-size: 0.82rem;
      font-weight: 700;
      text-decoration: none;
      white-space: nowrap;
    }
    .maintenance-month-tab:hover {
      background: rgba(255, 255, 255, 0.65);
      color: #334155;
    }
    .maintenance-month-tab.active {
      background: #fff;
      color: #1f4e79;
      box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
    }
    .maintenance-record-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      padding: 16px 20px 20px;
    }
    .maintenance-record-card {
      min-height: 168px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 16px 18px;
      background: #fff;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      box-shadow: 0 1px 0 rgba(15, 23, 42, 0.02);
      color: inherit;
      cursor: pointer;
      text-decoration: none;
      transition: background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
    }
    .maintenance-record-card:hover {
      background: #f8fbff;
      border-color: #94a3b8;
      box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
    }
    .maintenance-record-top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      min-width: 0;
    }
    .maintenance-title-group {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      min-width: 0;
    }
    .maintenance-customer-name {
      min-width: 0;
    }
    .maintenance-customer-name strong {
      display: block;
      overflow-wrap: anywhere;
      color: #111827;
      font-size: 1.08rem;
      font-weight: 700;
      line-height: 1.25;
    }
    .maintenance-record-side {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 8px;
      flex: 0 0 auto;
      color: #64748b;
      font-size: 0.8rem;
      white-space: nowrap;
    }
    .maintenance-card-date {
      color: #64748b;
      font-weight: 400;
    }
    .maintenance-card-arrow {
      color: #94a3b8;
      font-size: 0.72rem;
    }
    .maintenance-status-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
      min-height: 28px;
      padding: 0 9px;
      border-radius: 999px;
      font-size: 0.76rem;
      font-weight: 900;
      white-space: nowrap;
    }
    .maintenance-status-badge.due {
      background: #fff7ed;
      color: #b45309;
    }
    .maintenance-card-lines {
      display: grid;
      gap: 7px;
      padding-top: 2px;
    }
    .maintenance-card-line {
      display: flex;
      align-items: baseline;
      gap: 8px;
      min-width: 0;
      color: #1f2937;
      font-size: 0.88rem;
      line-height: 1.4;
    }
    .maintenance-line-label {
      flex: 0 0 auto;
      color: #64748b;
      font-size: 0.76rem;
      font-weight: 600;
    }
    .maintenance-line-value {
      min-width: 0;
      overflow-wrap: anywhere;
      font-weight: 400;
    }
    .maintenance-note {
      display: -webkit-box;
      margin: 0;
      overflow: hidden;
      color: #334155;
      font-size: 0.84rem;
      font-weight: 400;
      line-height: 1.5;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
    }
    .maintenance-manager {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-width: 0;
      color: #64748b;
      font-size: 0.82rem;
      font-weight: 400;
    }
    .maintenance-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 150px;
      margin: 16px 20px 20px;
      padding: 20px;
      border: 1px dashed #cbd5e1;
      border-radius: 8px;
      color: #64748b;
      font-weight: 800;
      text-align: center;
    }
    .vm-board-header {
      justify-content: space-between;
      flex-wrap: wrap;
    }
    .vm-board-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    .vm-board-title {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .vm-board-caption {
      color: #64748b;
      font-size: 0.92rem;
      font-weight: 500;
    }
    .vm-add-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 40px;
      padding: 0 16px;
      border-radius: 10px;
      border: 0;
      background: #475569;
      color: #fff;
      font-weight: 600;
      cursor: pointer;
    }
    .vm-add-btn:hover {
      background: #334155;
    }
    .vm-toggle-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 40px;
      padding: 0 16px;
      border: 0;
      border-radius: 10px;
      background: #e2e8f0;
      color: #0f172a;
      font-weight: 600;
      cursor: pointer;
    }
    .vm-toggle-btn:hover {
      background: #cbd5e1;
    }
    .vm-board-note {
      margin: 0 0 16px;
      color: #64748b;
      font-weight: 600;
    }
    .vm-board-body.is-collapsed {
      display: none;
    }
    .vm-message,
    .vm-error {
      margin-bottom: 16px;
      padding: 14px 16px;
      border-radius: 12px;
      font-weight: 600;
    }
    .vm-message {
      background: #ecfdf5;
      color: #166534;
      border: 1px solid #bbf7d0;
    }
    .vm-error {
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }
    .vm-table-wrap {
      border: 1px solid #e5edf6;
      border-radius: 14px;
      background: #fff;
      overflow-x: auto;
      overflow-y: hidden;
    }
    .vm-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 860px;
    }
    .vm-table th,
    .vm-table td {
      padding: 12px 10px;
      border-bottom: 1px solid #e5edf6;
      text-align: left;
      vertical-align: top;
    }
    .vm-table th {
      background: #f8fafc;
      color: #334155;
      font-size: 0.92rem;
    }
    .vm-row-clickable {
      cursor: pointer;
    }
    .vm-row-clickable:hover {
      background: #f8fbff;
    }
    .vm-modal-backdrop {
      position: fixed;
      inset: 0;
      display: none;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: rgba(15, 23, 42, 0.52);
      z-index: 2000;
    }
    .vm-modal-backdrop.is-open {
      display: flex;
    }
    .vm-modal {
      width: min(720px, 100%);
      max-height: calc(100vh - 48px);
      overflow: auto;
      background: #fff;
      border-radius: 18px;
      box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
    }
    .vm-modal-header,
    .vm-modal-footer {
      padding: 18px 22px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }
    .vm-modal-header {
      border-bottom: 1px solid #e5edf6;
    }
    .vm-modal-footer {
      border-top: 1px solid #e5edf6;
      flex-wrap: wrap;
    }
    .vm-modal-close {
      border: 0;
      background: transparent;
      font-size: 1.25rem;
      cursor: pointer;
      color: #475569;
    }
    .vm-modal-body {
      padding: 22px;
    }
    .vm-form {
      display: grid;
      gap: 16px;
    }
    .vm-form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }
    .vm-form label {
      display: grid;
      gap: 6px;
      font-weight: 600;
      color: #334155;
    }
    .vm-form input,
    .vm-form textarea {
      width: 100%;
      box-sizing: border-box;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      padding: 10px 12px;
      font: inherit;
      background: #fff;
    }
    .vm-form textarea {
      min-height: 110px;
      resize: vertical;
    }
    .vm-btn,
    .vm-btn-secondary,
    .vm-btn-danger {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 40px;
      padding: 0 16px;
      border: 0;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
      text-decoration: none;
    }
    .vm-btn {
      background: #1d4ed8;
      color: #fff;
    }
    .vm-btn-secondary {
      background: #e2e8f0;
      color: #0f172a;
    }
    .vm-btn-danger {
      background: #dc2626;
      color: #fff;
    }
    .vm-modal-footer-left,
    .vm-modal-footer-right {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      align-items: center;
    }
    body.vm-modal-open {
      overflow: hidden;
    }
    @media (max-width: 840px) {
      .vm-form-grid {
        grid-template-columns: 1fr;
      }
      .vm-modal-backdrop {
        padding: 12px;
      }
      .maintenance-month-header {
        align-items: flex-start;
        flex-direction: column;
      }
      .maintenance-month-actions {
        width: 100%;
        justify-content: space-between;
      }
      .maintenance-month-tabs {
        flex: 1 1 auto;
      }
      .maintenance-month-tab {
        flex: 1 1 0;
        min-width: 0;
        padding: 0 8px;
      }
      .maintenance-record-grid {
        grid-template-columns: 1fr;
      }
      .maintenance-record-top {
        gap: 10px;
      }
      .maintenance-record-side {
        align-items: flex-end;
        flex-direction: column;
        gap: 6px;
      }
    }
  </style>
</head>
<body class="page-1050 page-customers">
  <%@ include file="/WEB-INF/includes/header_nav.jspf" %>

  <div class="customer-management">
    <t:pageHeader>
      <jsp:attribute name="title"><i class="fas fa-th-large"></i> 대시보드</jsp:attribute>
      <jsp:attribute name="subtitle">
        업무 바로가기와 운영 메뉴를 한 화면에서 확인합니다.<br/>
        <span style="display:block; margin-top:6px; font-weight:600;">안녕하세요, <c:out value="${sessionScope.user != null ? sessionScope.user.userName : ''}"/> 님</span>
      </jsp:attribute>
    </t:pageHeader>

    <section class="maintenance-month-board" aria-label="이번 달 고객사 점검 현황">
      <div class="maintenance-month-header">
        <div class="maintenance-month-title">
          <i class="fas fa-clipboard-check"></i>
          <h2><c:out value="${maintenanceMonthLabel}" /> 고객사 점검 현황</h2>
        </div>
        <div class="maintenance-month-actions">
          <div class="maintenance-month-tabs" aria-label="점검 월 선택">
            <c:forEach var="monthTab" items="${maintenanceMonthTabs}">
              <c:url value="/dashboard" var="monthTabUrl">
                <c:param name="maintenanceMonth" value="${monthTab.value}" />
              </c:url>
              <a class="maintenance-month-tab ${monthTab.active ? 'active' : ''}" href="${monthTabUrl}">
                <c:out value="${monthTab.label}" />
              </a>
            </c:forEach>
          </div>
          <button type="button" class="maintenance-toggle-btn" id="toggleMaintenanceBoardBtn">접기</button>
        </div>
      </div>

      <div class="maintenance-month-body" id="maintenanceMonthBoardBody">
        <c:choose>
          <c:when test="${empty monthlyMaintenanceCards}">
            <div class="maintenance-empty">
              <c:out value="${maintenanceMonthLabel}" />에 등록된 정기점검 이력이 없습니다.
            </div>
          </c:when>
          <c:otherwise>
            <div class="maintenance-record-grid">
              <c:forEach var="record" items="${monthlyMaintenanceCards}">
                <c:url value="/maintenance" var="recordDetailUrl">
                  <c:param name="view" value="history" />
                  <c:param name="customerName" value="${record.customerName}" />
                </c:url>
                <a class="maintenance-record-card" href="${recordDetailUrl}">
                  <div class="maintenance-record-top">
                    <div class="maintenance-title-group">
                      <div class="maintenance-customer-name">
                        <strong><c:out value="${record.customerName}" /></strong>
                      </div>
                      <span class="maintenance-manager"><i class="fas fa-user-check"></i><c:out value="${record.inspectorName}" /></span>
                    </div>
                    <div class="maintenance-record-side">
                      <span class="maintenance-card-date"><c:out value="${record.inspectionDate}" /></span>
                      <c:if test="${record.statusCode == 'due'}">
                        <span class="maintenance-status-badge ${record.statusCode}">
                          <c:out value="${record.statusLabel}" />
                        </span>
                      </c:if>
                      <i class="fas fa-chevron-right maintenance-card-arrow"></i>
                    </div>
                  </div>
                  <div class="maintenance-card-lines">
                    <div class="maintenance-card-line">
                      <span class="maintenance-line-label">Vertica</span>
                      <span class="maintenance-line-value"><c:out value="${record.verticaVersion}" /></span>
                    </div>
                    <div class="maintenance-card-line">
                      <span class="maintenance-line-label">라이선스</span>
                      <span class="maintenance-line-value"><c:out value="${record.licenseSummary}" /></span>
                    </div>
                  </div>
                  <p class="maintenance-note"><c:out value="${record.note}" /></p>
                </a>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </section>

    <div class="card dashboard-card vm-board">
      <div class="card-header vm-board-header">
        <div class="vm-board-title">
          <i class="fas fa-network-wired"></i>
          <span>개인 호스트 관리</span>
          <span class="vm-board-caption">행을 클릭하면 수정 모달이 열립니다.</span>
        </div>
        <div class="vm-board-actions">
          <button type="button" class="vm-toggle-btn" id="toggleVmHostBoardBtn">접기</button>
          <button type="button" class="vm-add-btn" id="openVmHostAddBtn">호스트 등록</button>
        </div>
      </div>
      <div class="card-body vm-board-body" id="vmHostBoardBody">
        <c:if test="${param.vmHostResult == 'saved'}">
          <div class="vm-message">호스트 정보가 저장되었습니다.</div>
        </c:if>
        <c:if test="${param.vmHostResult == 'deleted'}">
          <div class="vm-message">호스트가 삭제되었습니다.</div>
        </c:if>
        <c:if test="${not empty vmHostErrorMessage}">
          <div class="vm-error"><c:out value="${vmHostErrorMessage}"/></div>
        </c:if>

        <p class="vm-board-note">컬럼은 사용 호스트, 목적, OS, VERTICA-ver, 원격지, 비고만 표시합니다.</p>

        <div class="vm-table-wrap">
          <table class="vm-table">
            <thead>
              <tr>
                <th>사용 호스트</th>
                <th>목적</th>
                <th>OS</th>
                <th>VERTICA-ver</th>
                <th>원격지</th>
                <th>비고</th>
              </tr>
            </thead>
            <tbody>
              <c:choose>
                <c:when test="${empty vmHosts}">
                  <tr>
                    <td colspan="6">등록된 VM 호스트가 없습니다.</td>
                  </tr>
                </c:when>
                <c:otherwise>
                  <c:forEach var="host" items="${vmHosts}">
                    <tr class="vm-row-clickable"
                        data-ip="<c:out value='${host.ip}'/>"
                        data-purpose="<c:out value='${host.purpose}'/>"
                        data-os-info="<c:out value='${host.osInfo}'/>"
                        data-vertica-version="<c:out value='${host.verticaVersion}'/>"
                        data-remote-host="<c:out value='${host.remoteHost}'/>"
                        data-note="<c:out value='${host.note}'/>">
                      <td><strong><c:out value="${host.ip}"/></strong></td>
                      <td><c:out value="${host.purpose}"/></td>
                      <td><c:out value="${host.osInfo}"/></td>
                      <td><c:out value="${host.verticaVersion}"/></td>
                      <td><c:out value="${host.remoteHost}"/></td>
                      <td><c:out value="${host.note}"/></td>
                    </tr>
                  </c:forEach>
                </c:otherwise>
              </c:choose>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card-grid">
      <c:forEach var="entry" items="${dashboardMenus}">
        <div class="card-item">
          <div class="card dashboard-card">
            <div class="card-header">
              <c:set var="cat" value="${entry.key}" />
              <c:choose>
                <c:when test="${cat == '고객 관리'}"><i class="fas fa-address-book"></i></c:when>
                <c:when test="${cat == '자료 관리'}"><i class="fas fa-folder-open"></i></c:when>
                <c:when test="${cat == '인프라 관리'}"><i class="fas fa-server"></i></c:when>
                <c:otherwise><i class="fas fa-th-large"></i></c:otherwise>
              </c:choose>
              ${entry.key}
            </div>
            <div class="card-body">
              <ul class="dashboard-submenu">
                <c:forEach var="menuItem" items="${entry.value}">
                  <li>
                    <a href="${pageContext.request.contextPath}/${menuItem.url}">
                      <i class="${menuItem.icon} fa-fw mr-2"></i>${menuItem.title}
                    </a>
                  </li>
                </c:forEach>
              </ul>
            </div>
          </div>
        </div>
      </c:forEach>

      <div class="card-item">
        <div class="card dashboard-card">
          <div class="card-header">
            <i class="fas fa-external-link-alt"></i> 바로가기
          </div>
          <div class="card-body">
            <ul class="dashboard-submenu">
              <li>
                <a href="https://docs.vertica.com/" target="_blank" rel="noopener noreferrer">
                  <i class="fas fa-link fa-fw mr-2"></i> Vertica 공식 문서
                </a>
              </li>
              <li>
                <a href="https://x2wizard.github.io/" target="_blank" rel="noopener noreferrer">
                  <i class="fas fa-link fa-fw mr-2"></i> Vertica 블로그
                </a>
              </li>
              <li>
                <a href="https://www.microfocus.com/lifecycle/" target="_blank" rel="noopener noreferrer">
                  <i class="fas fa-link fa-fw mr-2"></i> Vertica EOS 정보
                </a>
              </li>
              <li>
                <a href="https://portal.microfocus.com/s/customdetailpage" target="_blank" rel="noopener noreferrer">
                  <i class="fas fa-link fa-fw mr-2"></i> Vertica Case Open
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>

  <c:if test="${not empty vmHostForm}">
    <div id="vmHostFormSeed" hidden
         data-ip="<c:out value='${vmHostForm.ip}'/>"
         data-original-ip="<c:out value='${vmHostForm.ip}'/>"
         data-purpose="<c:out value='${vmHostForm.purpose}'/>"
         data-os-info="<c:out value='${vmHostForm.osInfo}'/>"
         data-vertica-version="<c:out value='${vmHostForm.verticaVersion}'/>"
         data-remote-host="<c:out value='${vmHostForm.remoteHost}'/>"
         data-note="<c:out value='${vmHostForm.note}'/>"></div>
  </c:if>

  <div class="vm-modal-backdrop" id="vmHostModalBackdrop">
    <div class="vm-modal" role="dialog" aria-modal="true" aria-labelledby="vmHostModalTitle">
      <div class="vm-modal-header">
        <h3 id="vmHostModalTitle">호스트 등록</h3>
        <button type="button" class="vm-modal-close" id="closeVmHostModalBtn" aria-label="닫기">×</button>
      </div>
      <div class="vm-modal-body">
        <form class="vm-form" id="vmHostSaveForm" method="post" action="${pageContext.request.contextPath}/vm-hosts">
          <input type="hidden" name="action" value="save" />
          <input type="hidden" name="returnTo" value="dashboard" />
          <input type="hidden" name="originalIp" id="vmHostOriginalIp" />

          <label>
            사용 호스트
            <input type="text" name="ip" id="vmHostIp" maxlength="15" placeholder="192.168.40.60" required />
          </label>

          <label>
            목적
            <input type="text" name="purpose" id="vmHostPurpose" maxlength="500" placeholder="예: 개인 개발 VM" required />
          </label>

          <div class="vm-form-grid">
            <label>
              OS
              <input type="text" name="osInfo" id="vmHostOsInfo" maxlength="100" placeholder="예: Ubuntu 22.04.5" />
            </label>

            <label>
              VERTICA-ver
              <input type="text" name="verticaVersion" id="vmHostVerticaVersion" maxlength="50" placeholder="예: 24.3.0-3" />
            </label>
          </div>

          <label>
            원격지
            <input type="text" name="remoteHost" id="vmHostRemoteHost" maxlength="100" placeholder="예: 192.168.40.160" />
          </label>

          <label>
            비고
            <textarea name="note" id="vmHostNote" placeholder="메모, 계정, 용도 등을 기록하세요."></textarea>
          </label>
        </form>
      </div>
      <div class="vm-modal-footer">
        <div class="vm-modal-footer-left">
          <form id="vmHostDeleteForm" method="post" action="${pageContext.request.contextPath}/vm-hosts" style="display:none;">
            <input type="hidden" name="action" value="delete" />
            <input type="hidden" name="returnTo" value="dashboard" />
            <input type="hidden" name="ip" id="vmHostDeleteIp" />
            <button type="submit" class="vm-btn-danger" onclick="return confirm('해당 호스트를 삭제하시겠습니까?');">삭제</button>
          </form>
        </div>
        <div class="vm-modal-footer-right">
          <button type="button" class="vm-btn-secondary" id="cancelVmHostModalBtn">취소</button>
          <button type="submit" class="vm-btn" form="vmHostSaveForm">저장</button>
        </div>
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/includes/footer_content.jspf" %>

  <script>
    (function() {
      const backdrop = document.getElementById('vmHostModalBackdrop');
      const boardBody = document.getElementById('vmHostBoardBody');
      const toggleBoardBtn = document.getElementById('toggleVmHostBoardBtn');
      const modalTitle = document.getElementById('vmHostModalTitle');
      const openAddBtn = document.getElementById('openVmHostAddBtn');
      const closeBtn = document.getElementById('closeVmHostModalBtn');
      const cancelBtn = document.getElementById('cancelVmHostModalBtn');
      const deleteForm = document.getElementById('vmHostDeleteForm');
      const originalIpInput = document.getElementById('vmHostOriginalIp');
      const ipInput = document.getElementById('vmHostIp');
      const purposeInput = document.getElementById('vmHostPurpose');
      const osInfoInput = document.getElementById('vmHostOsInfo');
      const verticaVersionInput = document.getElementById('vmHostVerticaVersion');
      const remoteHostInput = document.getElementById('vmHostRemoteHost');
      const noteInput = document.getElementById('vmHostNote');
      const deleteIpInput = document.getElementById('vmHostDeleteIp');
      const collapseStorageKey = 'frog2.dashboard.personal-hosts.collapsed';
      const maintenanceBody = document.getElementById('maintenanceMonthBoardBody');
      const toggleMaintenanceBtn = document.getElementById('toggleMaintenanceBoardBtn');
      const maintenanceCollapseStorageKey = 'frog2.dashboard.monthly-maintenance.collapsed';

      function setBoardCollapsed(collapsed) {
        boardBody.classList.toggle('is-collapsed', collapsed);
        toggleBoardBtn.textContent = collapsed ? '펼치기' : '접기';
        try {
          localStorage.setItem(collapseStorageKey, collapsed ? 'true' : 'false');
        } catch (ignore) {}
      }

      function setMaintenanceCollapsed(collapsed) {
        maintenanceBody.classList.toggle('is-collapsed', collapsed);
        toggleMaintenanceBtn.textContent = collapsed ? '펼치기' : '접기';
        try {
          localStorage.setItem(maintenanceCollapseStorageKey, collapsed ? 'true' : 'false');
        } catch (ignore) {}
      }

      function openModal() {
        backdrop.classList.add('is-open');
        document.body.classList.add('vm-modal-open');
      }

      function closeModal() {
        backdrop.classList.remove('is-open');
        document.body.classList.remove('vm-modal-open');
      }

      function populateModal(data, isEdit) {
        modalTitle.textContent = isEdit ? '호스트 수정' : '호스트 등록';
        originalIpInput.value = isEdit ? (data.originalIp || data.ip || '') : '';
        ipInput.value = data.ip || '';
        purposeInput.value = data.purpose || '';
        osInfoInput.value = data.osInfo || '';
        verticaVersionInput.value = data.verticaVersion || '';
        remoteHostInput.value = data.remoteHost || '';
        noteInput.value = data.note || '';
        deleteIpInput.value = isEdit ? (data.originalIp || data.ip || '') : '';
        deleteForm.style.display = isEdit ? 'block' : 'none';
        openModal();
      }

      openAddBtn.addEventListener('click', function() {
        populateModal({}, false);
      });

      toggleBoardBtn.addEventListener('click', function() {
        setBoardCollapsed(!boardBody.classList.contains('is-collapsed'));
      });

      toggleMaintenanceBtn.addEventListener('click', function() {
        setMaintenanceCollapsed(!maintenanceBody.classList.contains('is-collapsed'));
      });

      document.querySelectorAll('.vm-row-clickable').forEach(function(row) {
        row.addEventListener('click', function() {
          populateModal({
            ip: row.dataset.ip || '',
            originalIp: row.dataset.ip || '',
            purpose: row.dataset.purpose || '',
            osInfo: row.dataset.osInfo || '',
            verticaVersion: row.dataset.verticaVersion || '',
            remoteHost: row.dataset.remoteHost || '',
            note: row.dataset.note || ''
          }, true);
        });
      });

      closeBtn.addEventListener('click', closeModal);
      cancelBtn.addEventListener('click', closeModal);

      backdrop.addEventListener('click', function(event) {
        if (event.target === backdrop) {
          closeModal();
        }
      });

      document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && backdrop.classList.contains('is-open')) {
          closeModal();
        }
      });

      const formSeed = document.getElementById('vmHostFormSeed');
      if (formSeed) {
        populateModal({
          ip: formSeed.dataset.ip || '',
          originalIp: formSeed.dataset.originalIp || '',
          purpose: formSeed.dataset.purpose || '',
          osInfo: formSeed.dataset.osInfo || '',
          verticaVersion: formSeed.dataset.verticaVersion || '',
          remoteHost: formSeed.dataset.remoteHost || '',
          note: formSeed.dataset.note || ''
        }, !!formSeed.dataset.originalIp);
      }

      try {
        setBoardCollapsed(localStorage.getItem(collapseStorageKey) === 'true');
      } catch (ignore) {
        setBoardCollapsed(false);
      }

      try {
        setMaintenanceCollapsed(localStorage.getItem(maintenanceCollapseStorageKey) === 'true');
      } catch (ignore) {
        setMaintenanceCollapsed(false);
      }
    })();
  </script>
</body>
</html>
