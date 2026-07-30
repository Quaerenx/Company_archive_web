<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="마이페이지" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-mypage" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css,/resources/css/pages/mypage.css" scope="request" />

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
                   class="btn btn-primary btn-sm ui-button button--primary button--sm">
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

    <!-- 최근 점검 기록 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-clipboard-list"></i>
                최근 작성한 점검 기록
            </h2>
            <a href="${pageContext.request.contextPath}/maintenance"
               class="btn btn-primary btn-sm ui-button button--primary button--sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <c:choose>
                <c:when test="${not empty myMaintenanceRecords}">
                    <c:forEach var="record" items="${myMaintenanceRecords}" end="9">
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
    </div>

    <!-- 최근 트러블슈팅 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-wrench"></i>
                최근 작성한 트러블슈팅
            </h2>
            <a href="${pageContext.request.contextPath}/troubleshooting"
               class="btn btn-primary btn-sm ui-button button--primary button--sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <c:choose>
                <c:when test="${not empty myTroubleshootings}">
                    <c:forEach var="ts" items="${myTroubleshootings}" end="9">
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
    </div>
</div>

<%@ include file="/includes/footer.jsp" %>
