<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="pageTitle" value="정기점검 이력 - ${fn:escapeXml(customerName)}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance_history.css" scope="request" />
<c:set var="vendorScript" value="${pageContext.request.contextPath}/resources/vendor/chart.js/4.4.4/chart.umd.min.js?v=${frog2AssetVersion}" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_history.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="maintenance-history content-shell">
    <c:url value="/maintenance" var="addHistoryUrl">
        <c:param name="view" value="add"/>
        <c:param name="customerName" value="${customerName}"/>
    </c:url>
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-building"></i> <c:out value="${customerName}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <c:if test="${not empty customer}">
                <!-- <span class="detail-item"><i class="fas fa-calendar"></i> 도입년도: ${customer.firstIntroductionYear}</span>  -->
                <span class="detail-item"><i class="fas fa-database"></i> DB: <c:out value="${customer.dbName}" /></span>
                <span class="detail-item"><i class="fas fa-code-branch"></i> 버전: <c:out value="${customer.verticaVersion}" /></span>
                <span class="detail-item"><i class="fas fa-user"></i> 담당자: <c:out value="${customer.managerName}" /></span>
            </c:if>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${addHistoryUrl}"
               class="btn-min ui-button button--primary button--sm"><i class="fas fa-plus"></i> 새 점검 이력 추가</a>
            <a href="${pageContext.request.contextPath}/maintenance?view=cards"
               class="btn-min ui-button button--secondary button--sm"><i class="fas fa-arrow-left"></i> 목록으로</a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 라이선스 사용률 추이 차트 -->
    <div class="history-container history-chart-container">
        <div class="history-header">
            <div class="history-title" id="licenseUsageChartTitle"><i class="fas fa-chart-line"></i> 라이선스 사용률 추이</div>
            <div class="record-count">
                <c:choose>
                    <c:when test="${not empty usageSeries}">데이터: ${fn:length(usageSeries)}건</c:when>
                    <c:otherwise>데이터 없음</c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="usage-chart-wrap">
            <c:if test="${not empty usageSeries}">
                <c:set var="usagePointCount" value="${fn:length(usageSeries)}" />
                <c:set var="latestPoint" value="${usageSeries[usagePointCount - 1]}" />
                <c:if test="${usagePointCount > 1}">
                    <c:set var="previousPoint" value="${usageSeries[usagePointCount - 2]}" />
                </c:if>
                <p class="chart-summary" id="licenseUsageChartSummary">
                    최근 점검 <c:out value="${latestPoint.date}" /> 기준
                    <c:choose>
                        <c:when test="${latestPoint.pct ne null}">
                            사용률 <fmt:formatNumber value="${latestPoint.pct}" pattern="0.##" />%
                        </c:when>
                        <c:otherwise>사용률 정보 없음</c:otherwise>
                    </c:choose>
                    <c:if test="${latestPoint.usedTb ne null}">
                        · 사용량 <fmt:formatNumber value="${latestPoint.usedTb}" pattern="0.##" />TB
                    </c:if>
                    <c:if test="${latestPoint.sizeTb ne null}">
                        / 전체 <fmt:formatNumber value="${latestPoint.sizeTb}" pattern="0.##" />TB
                    </c:if>
                    <c:if test="${usagePointCount > 1 and latestPoint.pct ne null and previousPoint.pct ne null}">
                        · 이전 점검 대비
                        <fmt:formatNumber value="${latestPoint.pct - previousPoint.pct}" pattern="+0.##;-0.##" />%p
                    </c:if>
                </p>
            </c:if>
            <div id="licenseUsageSeries" hidden aria-hidden="true">
                <c:forEach var="pt" items="${usageSeries}">
                    <span data-usage-point
                          data-date="<c:out value='${pt.date}' />"
                          data-value="<c:out value='${pt.value}' />"
                          data-pct="<c:out value='${pt.pct}' />"
                          data-used-tb="<c:out value='${pt.usedTb}' />"
                          data-size-tb="<c:out value='${pt.sizeTb}' />"></span>
                </c:forEach>
            </div>
            <c:choose>
                <c:when test="${not empty usageSeries}">
                    <canvas id="licenseUsageChart"
                            height="120"
                            role="img"
                            aria-labelledby="licenseUsageChartTitle"
                            aria-describedby="licenseUsageChartSummary">
                        라이선스 사용률 추이를 날짜별로 표시한 선 그래프입니다.
                    </canvas>
                </c:when>
                <c:otherwise>
                    <div class="empty-history usage-chart-empty">
                        <i class="fas fa-chart-area"></i>
                        <h3>표시할 사용률 추이 데이터가 없습니다</h3>
                        <p>정기점검 이력을 추가하면 사용률 추이를 확인할 수 있습니다.</p>
                    </div>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty usageSeries}">
                <details class="chart-data-details">
                    <summary>차트 데이터 표로 보기</summary>
                    <div class="chart-data-table-wrap ui-table-wrap">
                        <table class="chart-data-table ui-table">
                            <caption>라이선스 사용률 추이 상세 데이터</caption>
                            <thead>
                                <tr>
                                    <th scope="col">점검일</th>
                                    <th scope="col">사용률(%)</th>
                                    <th scope="col">사용량(TB)</th>
                                    <th scope="col">전체 용량(TB)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="pt" items="${usageSeries}">
                                    <tr>
                                        <th scope="row"><c:out value="${pt.date}" /></th>
                                        <td><c:out value="${pt.pct}" default="-" /></td>
                                        <td><c:out value="${pt.usedTb}" default="-" /></td>
                                        <td><c:out value="${pt.sizeTb}" default="-" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </details>
            </c:if>
        </div>
    </div>

    <!-- 성공/에러 메시지 표시 -->
    <c:if test="${not empty sessionScope.message}">
        <div class="alert alert-success ui-alert ui-alert--success"
             role="status"
             aria-live="polite"
             aria-atomic="true">
            <i class="fas fa-check-circle"></i>
            <c:out value="${sessionScope.message}" />
        </div>
        <c:remove var="message" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-danger ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i>
            <c:out value="${sessionScope.error}" />
        </div>
        <c:remove var="error" scope="session" />
    </c:if>

    <!-- 정기점검 이력 목록 -->
    <div class="history-container">
        <div class="history-header">
            <div class="history-title">
                <i class="fas fa-clipboard-list"></i>
                정기점검 이력
            </div>
            <div class="record-count">
                총 <c:out value="${totalCount}" />건의 점검 이력
            </div>
        </div>

        <div class="history-grid">
            <c:choose>
                <c:when test="${not empty records}">
                    <c:forEach var="record" items="${records}">
                        <c:url var="maintenanceEditUrl" value="/maintenance">
                            <c:param name="view" value="edit" />
                            <c:param name="id" value="${record.maintenanceId}" />
                        </c:url>
                        <a class="history-item"
                           href="<c:out value='${maintenanceEditUrl}' />"
                           data-detail-url="<c:out value='${maintenanceEditUrl}' />">
                            <div class="history-meta">
                                <div class="inspection-date">
                                    <i class="fas fa-calendar-check"></i>
                                    <fmt:formatDate value="${record.inspectionDate}" pattern="yyyy년 MM월 dd일"/>
                                </div>
                                <div class="inspector-info">
                                    <div class="inspector-name"><c:out value="${record.inspectorName}" /></div>
                                    <div class="timestamp-info">
                                        등록: <fmt:formatDate value="${record.createdAt}" pattern="MM/dd HH:mm"/>
                                    </div>
                                </div>
                            </div>

                            <dl class="history-facts">
                                <div class="history-fact">
                                    <dt>Vertica 버전</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty record.verticaVersion}">
                                                <span class="version-tag ui-badge"><c:out value="${record.verticaVersion}" /></span>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div class="history-fact">
                                    <dt>라이선스</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty record.licenseSummary}">
                                                <c:out value="${record.licenseSummary}" />
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                            </dl>
                            <c:if test="${not empty record.note}">
                                <div class="history-note">
                                    <div class="note-label">점검 내용 및 비고</div>
                                    <div class="note-content"><c:out value="${record.note}" /></div>
                                </div>
                            </c:if>
                        </a>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="empty-history">
                        <i class="fas fa-clipboard"></i>
                        <h3>정기점검 이력이 없습니다</h3>
                        <p><c:out value="${customerName}" />의 정기점검 이력이 아직 등록되지 않았습니다.</p>
                        <a href="${addHistoryUrl}"
                           class="btn btn-secondary ui-button button--secondary button--md">
                            <i class="fas fa-plus"></i>
                            첫 번째 점검 이력 추가하기
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${currentPage > 1}">
            <c:url var="maintenanceHistoryPreviousUrl" value="/maintenance">
                <c:param name="view" value="history" />
                <c:param name="customerName" value="${customerName}" />
                <c:param name="historyPage" value="${currentPage - 1}" />
            </c:url>
        </c:if>
        <c:if test="${currentPage < totalPages}">
            <c:url var="maintenanceHistoryNextUrl" value="/maintenance">
                <c:param name="view" value="history" />
                <c:param name="customerName" value="${customerName}" />
                <c:param name="historyPage" value="${currentPage + 1}" />
            </c:url>
        </c:if>
        <t:tableFooter totalCount="${totalCount}"
                       itemLabel="점검 이력"
                       currentPage="${currentPage}"
                       totalPages="${totalPages}"
                       previousUrl="${maintenanceHistoryPreviousUrl}"
                       nextUrl="${maintenanceHistoryNextUrl}"
                       paginationLabel="정기점검 이력 페이지" />
    </div>
</div>

<!-- Chart.js -->


<%@ include file="/includes/footer.jsp" %>
