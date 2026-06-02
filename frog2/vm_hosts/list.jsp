<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="개인 호스트 관리" scope="request" />
<c:set var="pageBodyClass" value="page-1200 page-customers" scope="request" />

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
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
  <style>
    .vm-host-page {
      display: grid;
      gap: 24px;
    }
    .vm-host-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
    }
    .vm-stat-card {
      padding: 20px;
      border: 1px solid #dbe4f0;
      border-radius: 14px;
      background: #fff;
      box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
    }
    .vm-stat-card strong {
      display: block;
      margin-top: 8px;
      font-size: 1.75rem;
      color: #1d4ed8;
    }
    .vm-host-grid {
      display: grid;
      grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
      gap: 24px;
      align-items: start;
    }
    .vm-panel {
      background: #fff;
      border: 1px solid #dbe4f0;
      border-radius: 16px;
      box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05);
      overflow: hidden;
    }
    .vm-panel-header {
      padding: 18px 20px;
      border-bottom: 1px solid #e5edf6;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .vm-panel-body {
      padding: 20px;
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
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      padding: 10px 12px;
      font: inherit;
      background: #fff;
      box-sizing: border-box;
    }
    .vm-form textarea {
      min-height: 96px;
      resize: vertical;
    }
    .vm-form-hint {
      margin: 0;
      color: #64748b;
      font-size: 0.92rem;
    }
    .vm-form-actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
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
      text-decoration: none;
      cursor: pointer;
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
    .vm-message,
    .vm-error {
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
      overflow-x: auto;
    }
    .vm-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 880px;
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
    .vm-inline-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .vm-inline-actions form {
      margin: 0;
    }
    @media (max-width: 1080px) {
      .vm-host-grid {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 720px) {
      .vm-form-grid {
        grid-template-columns: 1fr;
      }
    }
  </style>
</head>
<body class="page-1200 page-customers">
  <%@ include file="/WEB-INF/includes/header_nav.jspf" %>

  <div class="customer-management">
    <t:pageHeader>
      <jsp:attribute name="title"><i class="fas fa-network-wired"></i> 개인 호스트 관리</jsp:attribute>
      <jsp:attribute name="subtitle">
        사용자별 VM 호스트를 등록, 수정, 삭제합니다.<br/>
        <span style="display:block; margin-top:6px; font-weight:600;">다른 사용자가 이미 등록한 IP와 중복될 수 없습니다.</span>
      </jsp:attribute>
    </t:pageHeader>

    <div class="vm-host-page">
      <div class="vm-host-stats">
        <div class="vm-stat-card">사용 중<strong><c:out value="${vmHostCount}"/></strong></div>
        <div class="vm-stat-card">남은 슬롯<strong><c:out value="${vmHostRemaining}"/></strong></div>
        <div class="vm-stat-card">한도<strong><c:out value="${vmHostLimit}"/></strong></div>
      </div>

      <c:if test="${param.result == 'saved'}">
        <div class="vm-message">호스트 정보가 저장되었습니다.</div>
      </c:if>
      <c:if test="${param.result == 'deleted'}">
        <div class="vm-message">호스트가 삭제되었습니다.</div>
      </c:if>
      <c:if test="${not empty errorMessage}">
        <div class="vm-error"><c:out value="${errorMessage}"/></div>
      </c:if>

      <div class="vm-host-grid">
        <section class="vm-panel">
          <div class="vm-panel-header">
            <i class="fas fa-edit"></i>
            <c:choose>
              <c:when test="${not empty editHost.ip}">호스트 수정</c:when>
              <c:otherwise>호스트 등록</c:otherwise>
            </c:choose>
          </div>
          <div class="vm-panel-body">
            <form class="vm-form" method="post" action="${pageContext.request.contextPath}/vm-hosts">
              <input type="hidden" name="action" value="save" />
              <input type="hidden" name="originalIp" value="<c:out value='${editHost.ip}'/>" />

              <label>
                IP 주소
                <input type="text" name="ip" value="<c:out value='${editHost.ip}'/>" placeholder="192.168.40.60" maxlength="15" required />
              </label>

              <label>
                사용 목적
                <input type="text" name="purpose" value="<c:out value='${editHost.purpose}'/>" placeholder="예: 개인 개발 VM" maxlength="500" required />
              </label>

              <div class="vm-form-grid">
                <label>
                  OS
                  <input type="text" name="osInfo" value="<c:out value='${editHost.osInfo}'/>" placeholder="예: Ubuntu 22.04.5" maxlength="100" />
                </label>
                <label>
                  Vertica 버전
                  <input type="text" name="verticaVersion" value="<c:out value='${editHost.verticaVersion}'/>" placeholder="예: 24.3.0-3" maxlength="50" />
                </label>
              </div>

              <label>
                원격지 / 연결 대상
                <input type="text" name="remoteHost" value="<c:out value='${editHost.remoteHost}'/>" placeholder="예: 192.168.40.160" maxlength="100" />
              </label>

              <label>
                비고
                <textarea name="note" placeholder="메모, 계정, 용도 등을 기록하세요."><c:out value="${editHost.note}"/></textarea>
              </label>

              <p class="vm-form-hint">허용 범위: 192.168.40.1 ~ 192.168.40.254, 사용자당 최대 20개</p>

              <div class="vm-form-actions">
                <button class="vm-btn" type="submit">저장</button>
                <a class="vm-btn-secondary" href="${pageContext.request.contextPath}/vm-hosts">새로 입력</a>
                <a class="vm-btn-secondary" href="${pageContext.request.contextPath}/dashboard">대시보드</a>
              </div>
            </form>
          </div>
        </section>

        <section class="vm-panel">
          <div class="vm-panel-header"><i class="fas fa-list"></i> 등록된 호스트 목록</div>
          <div class="vm-panel-body">
            <div class="vm-table-wrap">
              <table class="vm-table">
                <thead>
                  <tr>
                    <th>IP</th>
                    <th>목적</th>
                    <th>OS</th>
                    <th>VERTICA-ver</th>
                    <th>원격지</th>
                    <th>비고</th>
                    <th>수정일</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  <c:choose>
                    <c:when test="${empty vmHosts}">
                      <tr><td colspan="8">등록된 VM 호스트가 없습니다.</td></tr>
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
                            <c:choose>
                              <c:when test="${not empty host.updatedAt}">
                                <fmt:formatDate value="${host.updatedAt}" pattern="yyyy-MM-dd HH:mm" />
                              </c:when>
                              <c:otherwise>-</c:otherwise>
                            </c:choose>
                          </td>
                          <td>
                            <div class="vm-inline-actions">
                              <a class="vm-btn-secondary" href="${pageContext.request.contextPath}/vm-hosts?editIp=${host.ip}">수정</a>
                              <form method="post" action="${pageContext.request.contextPath}/vm-hosts" onsubmit="return confirm('해당 호스트를 삭제하시겠습니까?');">
                                <input type="hidden" name="action" value="delete" />
                                <input type="hidden" name="ip" value="<c:out value='${host.ip}'/>" />
                                <button class="vm-btn-danger" type="submit">삭제</button>
                              </form>
                            </div>
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
      </div>
    </div>
  </div>

  <%@ include file="/WEB-INF/includes/footer_content.jspf" %>
</body>
</html>
