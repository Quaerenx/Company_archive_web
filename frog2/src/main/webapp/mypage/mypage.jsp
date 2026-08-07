<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="마이페이지" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-mypage" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/mypage.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/mypage_hosts.js" scope="request" />
<c:set var="vmHostModalDescriptionIds" value="vmHostModalDescription" />
<c:if test="${not empty vmHostErrorMessage}">
    <c:set var="vmHostModalDescriptionIds" value="vmHostModalDescription vmHostModalError" />
</c:if>

<%@ include file="/includes/header.jsp" %>

<div class="mypage-container content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-user"></i> 마이페이지
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            내 정보 및 활동 현황
        </jsp:attribute>
    </t:pageHeader>

    <!-- 성공/에러 메시지 표시 -->
    <c:if test="${not empty message}">
        <c:set var="safeMessageTone" value="${messageType == 'success' ? 'success' : 'danger'}" />
        <c:set var="safeMessageIcon" value="${messageType == 'success' ? 'check-circle' : 'exclamation-circle'}" />
        <div class="ui-alert ui-alert--<c:out value='${safeMessageTone}' />"
             role="${messageType == 'success' ? 'status' : 'alert'}"
             aria-live="${messageType == 'success' ? 'polite' : 'assertive'}"
             aria-atomic="true">
            <i class="fas fa-<c:out value='${safeMessageIcon}' />"></i>
            <c:out value="${message}" />
        </div>
    </c:if>

    <!-- 프로필 카드 -->
    <div class="profile-card">
        <div class="profile-header">
            <h2>
                <i class="fas fa-user-circle"></i>
                내 정보
            </h2>
            <div class="profile-actions">
                <a href="mypage?action=editProfile"
                   class="btn btn-secondary btn-sm ui-button button--secondary button--sm">
                    <i class="fas fa-edit"></i>
                    프로필 수정
                </a>
                <a href="mypage?action=changePassword"
                   class="btn btn-secondary btn-sm ui-button button--secondary button--sm">
                    <i class="fas fa-key"></i>
                    비밀번호 변경
                </a>
            </div>
        </div>

        <div class="profile-info">
            <div class="info-item">
                <div class="info-label">아이디</div>
                <div class="info-value"><c:out value="${userInfo.userId}" /></div>
            </div>
            <div class="info-item">
                <div class="info-label">이름</div>
                <div class="info-value"><c:out value="${userInfo.userName}" /></div>
            </div>
        </div>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-section">
        <div class="stat-card">
            <i class="fas fa-clipboard-check mypage-stat-icon"></i>
            <div class="stat-label">작성한 점검 기록</div>
            <div class="stat-number"><c:out value="${empty maintenanceCount ? 0 : maintenanceCount}" /></div>
        </div>
        <div class="stat-card">
            <i class="fas fa-tools mypage-stat-icon"></i>
            <div class="stat-label">작성한 트러블슈팅</div>
            <div class="stat-number"><c:out value="${empty troubleshootingCount ? 0 : troubleshootingCount}" /></div>
        </div>
    </div>

    <!-- 바로가기 섹션 -->
    <div class="quick-links-section">
        <div class="quick-links-header">
            <h2>
                <i class="fas fa-th"></i>
                자주 사용하는 바로가기
            </h2>
        </div>
        <div class="quick-links-grid">
            <a href="${pageContext.request.contextPath}/mypage?action=monthlyResponse" class="quick-link-item">
                <i class="fas fa-calendar-alt quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">월별 고객 응대 현황</div>
                </div>
            </a>
            <a href="${pageContext.request.contextPath}/maintenance" class="quick-link-item">
                <i class="fas fa-clipboard-check quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">점검 기록 관리</div>
                </div>
            </a>
            <a href="${pageContext.request.contextPath}/troubleshooting" class="quick-link-item">
                <i class="fas fa-wrench quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">트러블슈팅 관리</div>
                </div>
            </a>
            <a href="${pageContext.request.contextPath}/customers" class="quick-link-item">
                <i class="fas fa-users quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">고객사 관리</div>
                </div>
            </a>
        </div>
    </div>

    <section class="mypage-host-card vm-board" aria-labelledby="vmHostBoardTitle">
        <div class="vm-board-header">
            <div class="vm-board-title">
                <div>
                    <h2 id="vmHostBoardTitle">개인 호스트 관리</h2>
                    <span class="vm-board-caption">
                        <c:out value="${vmHostCount}" default="0" /> / <c:out value="${vmHostLimit}" default="20" />개 사용
                    </span>
                </div>
            </div>
            <div class="vm-board-actions">
                <button type="button"
                        class="ui-button button--secondary button--sm"
                        id="toggleVmHostBoardBtn"
                        aria-controls="vmHostBoardBody"
                        aria-expanded="true">
                    접기
                </button>
                <button type="button"
                        class="ui-button button--secondary button--sm"
                        id="openVmHostAddBtn">
                    호스트 등록
                </button>
            </div>
        </div>
        <div class="vm-board-body" id="vmHostBoardBody">
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
                                        <td><strong><c:out value="${host.ip}" /></strong></td>
                                        <td><c:out value="${host.purpose}" /></td>
                                        <td><c:out value="${host.osInfo}" /></td>
                                        <td><c:out value="${host.verticaVersion}" /></td>
                                        <td><c:out value="${host.remoteHost}" /></td>
                                        <td><c:out value="${host.note}" /></td>
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

    <!-- 작성한 점검 기록 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-clipboard-list"></i>
                작성한 점검 기록
            </h2>
            <a href="${pageContext.request.contextPath}/maintenance"
               class="btn btn-secondary btn-sm ui-button button--secondary button--sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <c:choose>
                <c:when test="${not empty myMaintenanceRecords}">
                    <c:forEach var="record" items="${myMaintenanceRecords}">
                        <c:url var="maintenanceRecordUrl" value="/maintenance">
                            <c:param name="view" value="history" />
                            <c:param name="customerName" value="${record.customerName}" />
                        </c:url>
                        <div class="activity-item">
                            <div class="activity-title">
                                <a href="<c:out value='${maintenanceRecordUrl}' />">
                                    <i class="fas fa-building"></i>
                                    <c:out value="${record.customerName}" />
                                </a>
                            </div>
                            <div class="activity-meta">
                                <i class="far fa-calendar-alt"></i>
                                <c:choose>
                                    <c:when test="${not empty record.inspectionDate}"><fmt:formatDate value="${record.inspectionDate}" pattern="yyyy-MM-dd" /></c:when>
                                    <c:otherwise>날짜 없음</c:otherwise>
                                </c:choose>
                                <span class="activity-separator">|</span>
                                <i class="fas fa-server"></i> Vertica <c:out value="${not empty record.verticaVersion ? record.verticaVersion : '미기재'}" />
                            </div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <i class="fas fa-inbox"></i>
                        <p>작성한 점검 기록이 없습니다.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        <c:if test="${maintenanceTotalPages > 1}">
            <nav class="ui-pagination" aria-label="내 점검 기록 페이지">
                <div class="ui-pagination__summary">
                    <c:out value="${maintenancePage}" /> /
                    <c:out value="${maintenanceTotalPages}" /> 페이지
                </div>
                <c:set var="maintenanceStartPage" value="${maintenancePage - 2}" />
                <c:set var="maintenanceEndPage" value="${maintenancePage + 2}" />
                <c:if test="${maintenanceStartPage < 1}"><c:set var="maintenanceStartPage" value="1" /></c:if>
                <c:if test="${maintenanceEndPage > maintenanceTotalPages}"><c:set var="maintenanceEndPage" value="${maintenanceTotalPages}" /></c:if>
                <ul class="ui-pagination__links">
                    <c:if test="${maintenancePage > 1}">
                        <c:url var="maintenancePreviousPageUrl" value="/mypage">
                            <c:param name="action" value="view" />
                            <c:param name="maintenancePage" value="${maintenancePage - 1}" />
                            <c:param name="troubleshootingPage" value="${troubleshootingPage}" />
                        </c:url>
                        <li><a class="ui-pagination__link" href="<c:out value='${maintenancePreviousPageUrl}' />" aria-label="이전 점검 기록 페이지">&lsaquo;</a></li>
                    </c:if>
                    <c:forEach begin="${maintenanceStartPage}" end="${maintenanceEndPage}" var="pageNumber">
                        <c:choose>
                            <c:when test="${pageNumber == maintenancePage}">
                                <li><span class="ui-pagination__link" aria-current="page"><c:out value="${pageNumber}" /></span></li>
                            </c:when>
                            <c:otherwise>
                                <c:url var="maintenancePageUrl" value="/mypage">
                                    <c:param name="action" value="view" />
                                    <c:param name="maintenancePage" value="${pageNumber}" />
                                    <c:param name="troubleshootingPage" value="${troubleshootingPage}" />
                                </c:url>
                                <li><a class="ui-pagination__link" href="<c:out value='${maintenancePageUrl}' />"><c:out value="${pageNumber}" /></a></li>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                    <c:if test="${maintenancePage < maintenanceTotalPages}">
                        <c:url var="maintenanceNextPageUrl" value="/mypage">
                            <c:param name="action" value="view" />
                            <c:param name="maintenancePage" value="${maintenancePage + 1}" />
                            <c:param name="troubleshootingPage" value="${troubleshootingPage}" />
                        </c:url>
                        <li><a class="ui-pagination__link" href="<c:out value='${maintenanceNextPageUrl}' />" aria-label="다음 점검 기록 페이지">&rsaquo;</a></li>
                    </c:if>
                </ul>
            </nav>
        </c:if>
    </div>

    <!-- 작성한 트러블슈팅 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-wrench"></i>
                작성한 트러블슈팅
            </h2>
            <a href="${pageContext.request.contextPath}/troubleshooting"
               class="btn btn-secondary btn-sm ui-button button--secondary button--sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <c:choose>
                <c:when test="${not empty myTroubleshootings}">
                    <c:forEach var="ts" items="${myTroubleshootings}">
                        <c:url var="troubleshootingUrl" value="/troubleshooting">
                            <c:param name="view" value="view" />
                            <c:param name="id" value="${ts.id}" />
                        </c:url>
                        <div class="activity-item">
                            <div class="activity-title">
                                <a href="<c:out value='${troubleshootingUrl}' />">
                                    <i class="fas fa-file-alt"></i>
                                    <c:out value="${ts.title}" />
                                </a>
                            </div>
                            <div class="activity-meta">
                                <i class="fas fa-building"></i> <c:out value="${ts.customerName}" />
                                <span class="activity-separator">|</span>
                                <i class="far fa-calendar-alt"></i> 발생일:
                                <c:choose>
                                    <c:when test="${not empty ts.occurrenceDate}"><fmt:formatDate value="${ts.occurrenceDate}" pattern="yyyy-MM-dd" /></c:when>
                                    <c:otherwise>미기재</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <i class="fas fa-inbox"></i>
                        <p>작성한 트러블슈팅이 없습니다.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        <c:if test="${troubleshootingTotalPages > 1}">
            <nav class="ui-pagination" aria-label="내 트러블슈팅 페이지">
                <div class="ui-pagination__summary">
                    <c:out value="${troubleshootingPage}" /> /
                    <c:out value="${troubleshootingTotalPages}" /> 페이지
                </div>
                <c:set var="troubleshootingStartPage" value="${troubleshootingPage - 2}" />
                <c:set var="troubleshootingEndPage" value="${troubleshootingPage + 2}" />
                <c:if test="${troubleshootingStartPage < 1}"><c:set var="troubleshootingStartPage" value="1" /></c:if>
                <c:if test="${troubleshootingEndPage > troubleshootingTotalPages}"><c:set var="troubleshootingEndPage" value="${troubleshootingTotalPages}" /></c:if>
                <ul class="ui-pagination__links">
                    <c:if test="${troubleshootingPage > 1}">
                        <c:url var="myTroubleshootingPreviousPageUrl" value="/mypage">
                            <c:param name="action" value="view" />
                            <c:param name="maintenancePage" value="${maintenancePage}" />
                            <c:param name="troubleshootingPage" value="${troubleshootingPage - 1}" />
                        </c:url>
                        <li><a class="ui-pagination__link" href="<c:out value='${myTroubleshootingPreviousPageUrl}' />" aria-label="이전 트러블슈팅 페이지">&lsaquo;</a></li>
                    </c:if>
                    <c:forEach begin="${troubleshootingStartPage}" end="${troubleshootingEndPage}" var="pageNumber">
                        <c:choose>
                            <c:when test="${pageNumber == troubleshootingPage}">
                                <li><span class="ui-pagination__link" aria-current="page"><c:out value="${pageNumber}" /></span></li>
                            </c:when>
                            <c:otherwise>
                                <c:url var="myTroubleshootingPageUrl" value="/mypage">
                                    <c:param name="action" value="view" />
                                    <c:param name="maintenancePage" value="${maintenancePage}" />
                                    <c:param name="troubleshootingPage" value="${pageNumber}" />
                                </c:url>
                                <li><a class="ui-pagination__link" href="<c:out value='${myTroubleshootingPageUrl}' />"><c:out value="${pageNumber}" /></a></li>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                    <c:if test="${troubleshootingPage < troubleshootingTotalPages}">
                        <c:url var="myTroubleshootingNextPageUrl" value="/mypage">
                            <c:param name="action" value="view" />
                            <c:param name="maintenancePage" value="${maintenancePage}" />
                            <c:param name="troubleshootingPage" value="${troubleshootingPage + 1}" />
                        </c:url>
                        <li><a class="ui-pagination__link" href="<c:out value='${myTroubleshootingNextPageUrl}' />" aria-label="다음 트러블슈팅 페이지">&rsaquo;</a></li>
                    </c:if>
                </ul>
            </nav>
        </c:if>
    </div>
</div>

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
         aria-hidden="true"
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
                    <c:out value="${vmHostErrorMessage}" />
                </div>
            </c:if>
            <form class="vm-form ui-form"
                  id="vmHostSaveForm"
                  method="post"
                  data-ui-submit-lock="auto"
                  action="${pageContext.request.contextPath}/vm-hosts">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="save" />
                <input type="hidden" name="returnTo" value="mypage" />
                <input type="hidden" name="originalIp" id="vmHostOriginalIp" />

                <div class="ui-field">
                    <label for="vmHostIp">사용 호스트</label>
                    <input type="text"
                           name="ip"
                           id="vmHostIp"
                           maxlength="15"
                           placeholder="192.168.40.60"
                           data-dialog-initial-focus
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
                    <input type="hidden" name="returnTo" value="mypage" />
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
