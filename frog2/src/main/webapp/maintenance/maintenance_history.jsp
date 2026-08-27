<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="pageTitle" value="정기점검 이력 - ${fn:escapeXml(customerName)}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance_history.css" scope="request" />
<c:set var="vendorScript" value="${pageContext.request.contextPath}/resources/vendor/chart.js/4.4.4/chart.umd.min.js?v=${initParam.frog2AssetVersion}" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_history.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="maintenance-history content-shell">
    <c:url value="/maintenance" var="addHistoryUrl">
        <c:param name="view" value="add"/>
        <c:param name="customerName" value="${customerName}"/>
    </c:url>
    <c:url value="/customers" var="customerDetailUrl">
        <c:param name="view" value="detail" />
        <c:param name="customerName" value="${customerName}" />
    </c:url>
    <t:pageHeader>
        <jsp:attribute name="title">
            <a class="maintenance-customer-title-link"
               href="<c:out value='${customerDetailUrl}' />">
                <i class="fas fa-building" aria-hidden="true"></i>
                <span><c:out value="${customerName}" /></span>
            </a>
        </jsp:attribute>
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
               class="ui-button button--primary button--sm"><i class="fas fa-plus"></i> 새 점검 이력 추가</a>
            <a href="${pageContext.request.contextPath}/maintenance?view=cards"
               class="ui-button button--secondary button--sm"><i class="fas fa-arrow-left"></i> 목록으로</a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 라이선스 사용률 추이 차트 -->
    <div class="history-container history-chart-container ui-work-surface">
        <t:sectionHeader className="history-header">
            <jsp:attribute name="title">
                <h2 class="history-title ui-section-title" id="licenseUsageChartTitle"><i class="fas fa-chart-line"></i> 라이선스 사용률 추이</h2>
            </jsp:attribute>
            <jsp:attribute name="actions">
                <div class="record-count">
                <c:choose>
                    <c:when test="${not empty usageSeries}">데이터: ${fn:length(usageSeries)}건</c:when>
                    <c:otherwise>데이터 없음</c:otherwise>
                </c:choose>
                </div>
            </jsp:attribute>
        </t:sectionHeader>
        <div class="usage-chart-wrap ui-section-body">
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
                            사용률 <fmt:formatNumber value="${latestPoint.pct}" pattern="0.0" />%
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
                        <fmt:formatNumber value="${latestPoint.pct - previousPoint.pct}" pattern="+0.0;-0.0" />%p
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
                    <div class="usage-chart-scroll"
                         data-ui-scroll-region
                         data-ui-scroll-label="라이선스 사용률 추이 차트">
                        <div class="usage-chart-canvas">
                            <canvas id="licenseUsageChart"
                                    height="120"
                                    role="img"
                                    aria-labelledby="licenseUsageChartTitle"
                                    aria-describedby="licenseUsageChartSummary">
                                라이선스 사용률 추이를 날짜별로 표시한 선 그래프입니다.
                            </canvas>
                        </div>
                    </div>
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

    <t:flashMessages />

    <!-- 정기점검 이력 목록 -->
    <div class="history-container ui-work-surface">
        <t:sectionHeader className="history-header">
            <jsp:attribute name="title">
                <h2 class="history-title ui-section-title">
                    <i class="fas fa-clipboard-list"></i>
                    정기점검 이력
                </h2>
            </jsp:attribute>
            <jsp:attribute name="actions">
                <div class="record-count">
                    <c:out value="${totalCount}" />건
                </div>
            </jsp:attribute>
        </t:sectionHeader>

        <c:url var="maintenanceHistoryResetUrl" value="/maintenance">
            <c:param name="view" value="history" />
            <c:param name="customerName" value="${customerName}" />
        </c:url>
        <form class="history-filter-form ui-table-toolbar ui-form ui-form--compact"
              method="get"
              action="${pageContext.request.contextPath}/maintenance"
              aria-label="정기점검 이력 필터">
            <input type="hidden" name="view" value="history" />
            <input type="hidden" name="customerName" value="${fn:escapeXml(customerName)}" />
            <label class="history-filter-field history-filter-field--year">
                <span>연도</span>
                <input type="number"
                       name="historyYear"
                       min="1900"
                       max="2100"
                       inputmode="numeric"
                       value="${fn:escapeXml(historyYear)}"
                       placeholder="전체" />
            </label>
            <label class="history-filter-field history-filter-field--version">
                <span>버전</span>
                <input type="search"
                       name="historyVersion"
                       maxlength="64"
                       value="${fn:escapeXml(historyVersion)}"
                       placeholder="예: 23.4" />
            </label>
            <label class="history-filter-field history-filter-field--query">
                <span>키워드</span>
                <input type="search"
                       name="historyQuery"
                       maxlength="120"
                       value="${fn:escapeXml(historyQuery)}"
                       placeholder="점검자, 버전, 점검 메모" />
            </label>
            <div class="history-filter-actions">
                <button type="submit"
                        class="ui-button button--secondary button--sm">검색</button>
                <c:if test="${historyFiltersActive}">
                    <a class="ui-button button--secondary button--sm"
                       href="<c:out value='${maintenanceHistoryResetUrl}' />">초기화</a>
                </c:if>
            </div>
        </form>

        <c:choose>
            <c:when test="${not empty historyRows}">
                <p class="history-scroll-hint" id="historyScrollHint" hidden>
                    표를 좌우로 스크롤해 전체 항목을 확인할 수 있습니다.
                </p>
                <div class="history-table-scroll ui-table-wrap"
                     data-ui-scroll-region
                     data-ui-scroll-label="정기점검 이력 비교표"
                     data-ui-scroll-hint-id="historyScrollHint">
                    <table class="history-comparison-table ui-table ui-data-table">
                        <caption class="sr-only">
                            <c:out value="${customerName}" /> 정기점검 이력 비교표
                        </caption>
                        <thead>
                            <tr>
                                <th scope="col">점검일</th>
                                <th scope="col">버전</th>
                                <th scope="col">라이선스 사용량</th>
                                <th scope="col">이전 대비</th>
                                <th scope="col" class="history-col-inspector">점검자</th>
                                <th scope="col" class="history-col-note">점검 내용</th>
                            </tr>
                        </thead>
                        <c:forEach var="row" items="${historyRows}">
                            <c:url var="maintenanceEditUrl" value="/maintenance">
                                <c:param name="view" value="edit" />
                                <c:param name="id" value="${row.record.maintenanceId}" />
                            </c:url>
                            <c:choose>
                                <c:when test="${not empty row.record.inspectionDate}">
                                    <fmt:formatDate var="inspectionDateLabel"
                                                    value="${row.record.inspectionDate}"
                                                    pattern="yyyy.MM.dd" />
                                </c:when>
                                <c:otherwise>
                                    <c:set var="inspectionDateLabel" value="날짜 미등록" />
                                </c:otherwise>
                            </c:choose>
                            <tbody class="history-record-group">
                                <tr class="history-summary-row" data-ui-disclosure-row>
                                    <th class="history-date-cell" scope="row">
                                        <button type="button"
                                                class="history-row-toggle"
                                                data-ui-disclosure-toggle
                                                aria-expanded="false"
                                                aria-controls="${row.detailId}"
                                                aria-label="${inspectionDateLabel} 정기점검 상세">
                                            <c:choose>
                                                <c:when test="${not empty row.record.inspectionDate}">
                                                    <fmt:formatDate value="${row.record.inspectionDate}" pattern="yyyy.MM.dd" />
                                                </c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </button>
                                    </th>
                                    <td class="history-version-cell">
                                        <c:choose>
                                            <c:when test="${not empty row.record.verticaVersion}">
                                                <span class="version-tag ui-badge ui-badge--neutral"><c:out value="${row.record.verticaVersion}" /></span>
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="history-license-cell">
                                        <div class="history-license-values">
                                            <span>
                                                <c:choose>
                                                    <c:when test="${not empty row.usedTerabytes and not empty row.capacityTerabytes}">
                                                        <strong><c:out value="${row.usedTerabytes}" /></strong>
                                                        / <c:out value="${row.capacityTerabytes}" /> TB
                                                    </c:when>
                                                    <c:when test="${not empty row.usedTerabytes}">
                                                        <strong><c:out value="${row.usedTerabytes}" /></strong> TB 사용
                                                    </c:when>
                                                    <c:when test="${not empty row.capacityTerabytes}">
                                                        <c:out value="${row.capacityTerabytes}" /> TB 한도
                                                    </c:when>
                                                    <c:otherwise>—</c:otherwise>
                                                </c:choose>
                                            </span>
                                            <c:if test="${not empty row.usagePercentage}">
                                                <span class="history-license-percent history-license-percent--${row.usageTone}">
                                                    <c:out value="${row.usagePercentage}" />%
                                                    <c:if test="${row.usageTone eq 'warning' or row.usageTone eq 'risk'}">
                                                        <span class="history-license-status-label"><c:out value="${row.usageStatusLabel}" /></span>
                                                    </c:if>
                                                </span>
                                            </c:if>
                                        </div>
                                        <c:if test="${not empty row.usagePercentage}">
                                            <progress class="history-license-progress history-license-progress--${row.usageTone}"
                                                      max="100"
                                                      value="${row.usageProgressPercentage}"
                                                      aria-label="라이선스 사용률 ${row.usagePercentage}%, ${row.usageStatusLabel}"><c:out value="${row.usagePercentage}" />%</progress>
                                        </c:if>
                                    </td>
                                    <td class="history-number-cell">
                                        <span class="history-delta">
                                            <c:out value="${row.deltaLabel}" />
                                        </span>
                                    </td>
                                    <td class="history-inspector-cell history-col-inspector">
                                        <c:out value="${row.record.inspectorName}" default="—" />
                                    </td>
                                    <td class="history-note-summary-cell history-col-note">
                                        <c:out value="${row.noteSummary}" />
                                    </td>
                                </tr>
                                <tr id="${row.detailId}" class="history-detail-row" hidden>
                                    <td class="history-detail-cell ui-disclosure-cell" colspan="6">
                                        <div class="history-detail-motion"
                                             data-ui-disclosure-content>
                                            <div class="ui-disclosure-clip">
                                              <div class="history-detail-content">
                                                <section class="history-detail-section history-detail-summary"
                                                         aria-label="라이선스 요약">
                                                    <h3>라이선스 요약</h3>
                                                    <dl class="history-detail-metrics">
                                                        <div>
                                                            <dt>라이선스 사용량</dt>
                                                            <dd>
                                                                <c:choose>
                                                                    <c:when test="${not empty row.usedTerabytes and not empty row.capacityTerabytes}">
                                                                        <c:out value="${row.usedTerabytes}" /> / <c:out value="${row.capacityTerabytes}" /> TB
                                                                    </c:when>
                                                                    <c:when test="${not empty row.usedTerabytes}">
                                                                        <c:out value="${row.usedTerabytes}" /> TB
                                                                    </c:when>
                                                                    <c:when test="${not empty row.capacityTerabytes}">
                                                                        한도 <c:out value="${row.capacityTerabytes}" /> TB
                                                                    </c:when>
                                                                    <c:otherwise>—</c:otherwise>
                                                                </c:choose>
                                                            </dd>
                                                        </div>
                                                        <div>
                                                            <dt>라이선스 사용률</dt>
                                                            <dd><c:out value="${row.usagePercentage}" default="—" /><c:if test="${not empty row.usagePercentage}">%</c:if></dd>
                                                        </div>
                                                        <div>
                                                            <dt>전월 사용률</dt>
                                                            <dd><c:out value="${row.previousUsagePercentage}" default="—" /><c:if test="${not empty row.previousUsagePercentage}">%</c:if></dd>
                                                        </div>
                                                        <div>
                                                            <dt>전월 대비</dt>
                                                            <dd><c:out value="${row.deltaLabel}" /></dd>
                                                        </div>
                                                    </dl>
                                                </section>
                                                <div class="history-detail-lower">
                                                    <section class="history-detail-section history-detail-note-section">
                                                        <h3>점검 메모</h3>
                                                        <div class="history-detail-note"><c:choose><c:when test="${not empty row.record.note}"><c:out value="${row.record.note}" /></c:when><c:otherwise>특이사항 없음</c:otherwise></c:choose></div>
                                                    </section>
                                                    <section class="history-detail-section history-detail-meta-section">
                                                        <h3>등록 정보</h3>
                                                        <dl class="history-detail-meta">
                                                            <div>
                                                                <dt>점검자</dt>
                                                                <dd><c:out value="${row.record.inspectorName}" default="—" /></dd>
                                                            </div>
                                                            <div>
                                                                <dt>등록 일시</dt>
                                                                <dd>
                                                                    <c:choose>
                                                                        <c:when test="${not empty row.record.createdAt}">
                                                                            <fmt:formatDate value="${row.record.createdAt}" pattern="yyyy.MM.dd HH:mm" />
                                                                        </c:when>
                                                                        <c:otherwise>—</c:otherwise>
                                                                    </c:choose>
                                                                </dd>
                                                            </div>
                                                            <c:if test="${not empty row.record.updatedAt}">
                                                                <div>
                                                                    <dt>수정 일시</dt>
                                                                    <dd><fmt:formatDate value="${row.record.updatedAt}" pattern="yyyy.MM.dd HH:mm" /></dd>
                                                                </div>
                                                            </c:if>
                                                        </dl>
                                                        <a class="ui-button button--secondary button--sm"
                                                           href="<c:out value='${maintenanceEditUrl}' />">이력 수정</a>
                                                    </section>
                                                </div>
                                              </div>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </c:forEach>
                    </table>
                </div>
                <c:if test="${currentPage > 1}">
                    <c:url var="maintenanceHistoryPreviousUrl" value="/maintenance">
                        <c:param name="view" value="history" />
                        <c:param name="customerName" value="${customerName}" />
                        <c:param name="historyPage" value="${currentPage - 1}" />
                        <c:param name="historyYear" value="${historyYear}" />
                        <c:param name="historyVersion" value="${historyVersion}" />
                        <c:param name="historyQuery" value="${historyQuery}" />
                    </c:url>
                </c:if>
                <c:if test="${currentPage < totalPages}">
                    <c:url var="maintenanceHistoryNextUrl" value="/maintenance">
                        <c:param name="view" value="history" />
                        <c:param name="customerName" value="${customerName}" />
                        <c:param name="historyPage" value="${currentPage + 1}" />
                        <c:param name="historyYear" value="${historyYear}" />
                        <c:param name="historyVersion" value="${historyVersion}" />
                        <c:param name="historyQuery" value="${historyQuery}" />
                    </c:url>
                </c:if>
                <t:tableFooter itemLabel="점검 이력"
                               currentPage="${currentPage}"
                               totalPages="${totalPages}"
                               previousUrl="${maintenanceHistoryPreviousUrl}"
                               nextUrl="${maintenanceHistoryNextUrl}"
                               paginationLabel="정기점검 이력 페이지" />
            </c:when>
            <c:otherwise>
                <div class="empty-history ui-empty-state">
                    <i class="fas fa-clipboard" aria-hidden="true"></i>
                    <c:choose>
                        <c:when test="${historyFiltersActive}">
                            <strong>검색 결과가 없습니다</strong>
                            <span>다른 연도, 버전 또는 키워드로 검색해 주세요.</span>
                            <a href="<c:out value='${maintenanceHistoryResetUrl}' />"
                               class="ui-button button--secondary button--md">검색 조건 초기화</a>
                        </c:when>
                        <c:otherwise>
                            <strong>정기점검 이력이 없습니다</strong>
                            <span><c:out value="${customerName}" />의 정기점검 이력이 아직 등록되지 않았습니다.</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<!-- Chart.js -->


<%@ include file="/includes/footer.jsp" %>
