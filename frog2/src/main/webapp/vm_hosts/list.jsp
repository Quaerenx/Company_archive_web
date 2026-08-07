<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="개인 호스트 관리" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-vm-hosts" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/vm_hosts.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/vm_hosts.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

  <div class="content-management content-shell">
    <t:pageHeader>
      <jsp:attribute name="title"><i class="fas fa-network-wired"></i> 개인 호스트 관리</jsp:attribute>
      <jsp:attribute name="subtitle">
        사용자별 VM 호스트를 등록, 수정, 삭제합니다.<br/>
        <span class="vm-host-duplicate-note">다른 사용자가 이미 등록한 IP와 중복될 수 없습니다.</span>
      </jsp:attribute>
    </t:pageHeader>

    <div class="vm-host-page">
      <div class="vm-host-stats">
        <div class="vm-stat-card">사용 중<strong><c:out value="${vmHostCount}"/></strong></div>
        <div class="vm-stat-card">남은 슬롯<strong><c:out value="${vmHostRemaining}"/></strong></div>
        <div class="vm-stat-card">한도<strong><c:out value="${vmHostLimit}"/></strong></div>
      </div>

      <c:if test="${param.result == 'saved'}">
        <div class="vm-message ui-alert ui-alert--success"
             role="status"
             aria-live="polite"
             aria-atomic="true">호스트 정보가 저장되었습니다.</div>
      </c:if>
      <c:if test="${param.result == 'deleted'}">
        <div class="vm-message ui-alert ui-alert--success"
             role="status"
             aria-live="polite"
             aria-atomic="true">호스트가 삭제되었습니다.</div>
      </c:if>
      <c:if test="${not empty errorMessage}">
        <div class="vm-error ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true"><c:out value="${errorMessage}"/></div>
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
            <form class="vm-form ui-form"
                  method="post"
                  action="${pageContext.request.contextPath}/vm-hosts"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
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
                <button class="vm-btn ui-button button--primary button--md"
                        type="submit"
                        data-busy-label="저장 중">저장</button>
                <a class="vm-btn-secondary ui-button button--secondary button--md"
                   href="${pageContext.request.contextPath}/vm-hosts">새로 입력</a>
                <a class="vm-btn-secondary ui-button button--secondary button--md"
                   href="${pageContext.request.contextPath}/dashboard">대시보드</a>
              </div>
            </form>
          </div>
        </section>

        <section class="vm-panel">
          <div class="vm-panel-header"><i class="fas fa-list"></i> 등록된 호스트 목록</div>
          <div class="vm-panel-body">
            <div class="vm-table-wrap ui-table-wrap">
              <table class="vm-table ui-table">
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
                              <a class="vm-btn-secondary ui-button button--secondary button--sm"
                                 href="${pageContext.request.contextPath}/vm-hosts?editIp=${host.ip}">수정</a>
                              <form class="js-vm-host-delete ui-form"
                                    method="post"
                                    action="${pageContext.request.contextPath}/vm-hosts"
                                    data-ui-submit-lock="auto">
                                  <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                                <input type="hidden" name="action" value="delete" />
                                <input type="hidden" name="ip" value="<c:out value='${host.ip}'/>" />
                                <button class="vm-btn-danger ui-button button--danger button--sm"
                                        type="submit"
                                        data-busy-label="삭제 중">삭제</button>
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

<%@ include file="/includes/footer.jsp" %>
