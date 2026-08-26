<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="고객사 히스토리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customer-history" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customer_history.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_history.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<c:url var="addHistoryUrl" value="/customer-history">
    <c:param name="view" value="add" />
    <c:param name="customerName" value="${customerName}" />
    <c:param name="returnCustomerName" value="${customerName}" />
    <c:param name="returnCategory" value="${category}" />
    <c:param name="returnQ" value="${q}" />
    <c:param name="returnPage" value="${currentPage}" />
</c:url>

<div class="customer-history content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-history" aria-hidden="true"></i>
            고객사 히스토리
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            주요 장애, 업그레이드, 증설 등 고객사 작업 이력을 관리합니다.
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${addHistoryUrl}' />"
               class="ui-button button--primary button--md">
                <i class="fas fa-plus" aria-hidden="true"></i>
                이력 등록
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <section class="customer-history-panel ui-work-surface"
             aria-labelledby="customer-history-list-title">
        <h2 id="customer-history-list-title" class="sr-only">고객사 작업 이력 목록</h2>
        <form class="customer-history-filter-form ui-table-toolbar ui-form ui-form--compact"
              method="get"
              action="${pageContext.request.contextPath}/customer-history"
              data-ui-submit-lock="auto">
            <label class="customer-history-filter-field">
                <span class="sr-only">고객사</span>
                <select name="customerName">
                    <option value="">전체 고객사</option>
                    <c:forEach var="customer" items="${customerList}">
                        <option value="<c:out value='${customer.customerName}' />"
                                ${customerName eq customer.customerName ? 'selected' : ''}>
                            <c:out value="${customer.customerName}" />
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label class="customer-history-filter-field">
                <span class="sr-only">작업 유형</span>
                <select name="category">
                    <option value="all">전체 유형</option>
                    <c:forEach var="historyCategory" items="${historyCategories}">
                        <option value="${historyCategory.code}"
                                ${category eq historyCategory.code ? 'selected' : ''}>
                            <c:out value="${historyCategory.label}" />
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label class="customer-history-filter-field customer-history-filter-field--query">
                <span class="sr-only">검색</span>
                <input type="search"
                       name="q"
                       value="<c:out value='${q}' />"
                       maxlength="100"
                       autocomplete="off"
                       placeholder="이력 또는 조치사항 검색">
            </label>
            <div class="customer-history-filter-actions">
                <button type="submit"
                        class="ui-button button--secondary button--sm customer-history-search-button"
                        data-busy-label="검색 중">검색</button>
                <c:if test="${not empty customerName || (not empty category && category ne 'all') || not empty q}">
                    <a href="${pageContext.request.contextPath}/customer-history"
                       class="ui-button button--ghost button--sm">초기화</a>
                </c:if>
            </div>
        </form>

        <c:choose>
            <c:when test="${not empty historyRecords}">
                <div class="ui-table-wrap customer-history-table-wrap"
                     data-ui-scroll-region
                     data-ui-scroll-label="고객사 히스토리 표">
                    <table class="ui-table ui-data-table customer-history-table">
                        <caption class="sr-only">주요 고객사 작업 이력</caption>
                        <thead>
                            <tr>
                                <th scope="col" class="col--date">날짜</th>
                                <th scope="col" class="col--customer">고객사</th>
                                <th scope="col" class="col--type">유형</th>
                                <th scope="col" class="col--work">작업 내용</th>
                                <th scope="col" class="col--status">상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="history" items="${historyRecords}">
                                <c:url var="editHistoryUrl" value="/customer-history">
                                    <c:param name="view" value="edit" />
                                    <c:param name="id" value="${history.id}" />
                                    <c:param name="returnCustomerName" value="${customerName}" />
                                    <c:param name="returnCategory" value="${category}" />
                                    <c:param name="returnQ" value="${q}" />
                                    <c:param name="returnPage" value="${currentPage}" />
                                </c:url>
                                <tr class="customer-history-summary-row"
                                    data-ui-disclosure-row>
                                    <td class="col--date">
                                        <time datetime="<c:out value='${history.workDate}' />">
                                            <c:out value="${history.workDate}" />
                                        </time>
                                    </td>
                                    <td class="col--customer">
                                        <strong><c:out value="${history.customerName}" /></strong>
                                    </td>
                                    <td class="col--type">
                                        <span class="customer-history-category customer-history-category--${history.category.code}">
                                            <c:out value="${history.category.label}" />
                                        </span>
                                    </td>
                                    <td class="col--work">
                                        <button type="button"
                                                class="customer-history-detail-toggle"
                                                data-ui-disclosure-toggle
                                                aria-expanded="false"
                                                aria-controls="customer-history-detail-<c:out value='${history.id}' />"
                                                aria-label="<c:out value='${history.customerName}' /> <c:out value='${history.workDate}' /> <c:out value='${history.title}' /> 이력 상세">
                                            <span class="customer-history-work" aria-hidden="true">
                                                <strong><c:out value="${history.title}" /></strong>
                                                <c:if test="${history.actionSummary ne history.title}">
                                                    <span><c:out value="${history.actionSummary}" /></span>
                                                </c:if>
                                            </span>
                                        </button>
                                    </td>
                                    <td class="col--status">
                                        <div class="customer-history-status-actions">
                                            <span class="customer-history-status customer-history-status--${history.status.tone}">
                                                <c:out value="${history.status.label}" />
                                            </span>
                                            <c:if test="${history.creatorUserId eq currentUserId}">
                                                <a href="<c:out value='${editHistoryUrl}' />"
                                                   class="customer-history-edit-link ui-button button--ghost button--sm"
                                                   title="이력 수정">
                                                    <i class="fas fa-pen" aria-hidden="true"></i>
                                                    <span class="sr-only"><c:out value="${history.customerName}" /> <c:out value="${history.workDate}" /> 이력 수정</span>
                                                </a>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                                <tr id="customer-history-detail-<c:out value='${history.id}' />"
                                    class="customer-history-detail-row"
                                    hidden>
                                    <td colspan="5">
                                        <div class="customer-history-detail-motion"
                                             data-ui-disclosure-content>
                                            <dl class="customer-history-detail">
                                                <div>
                                                    <dt>작업 내용</dt>
                                                    <dd><strong><c:out value="${history.title}" /></strong></dd>
                                                </div>
                                                <div>
                                                    <dt>조치사항</dt>
                                                    <dd><c:out value="${history.actionSummary}" /></dd>
                                                </div>
                                            </dl>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <c:if test="${currentPage > 1}">
                    <c:url var="previousHistoryUrl" value="/customer-history">
                        <c:param name="customerName" value="${customerName}" />
                        <c:param name="category" value="${category}" />
                        <c:param name="q" value="${q}" />
                        <c:param name="page" value="${currentPage - 1}" />
                    </c:url>
                </c:if>
                <c:if test="${currentPage < totalPages}">
                    <c:url var="nextHistoryUrl" value="/customer-history">
                        <c:param name="customerName" value="${customerName}" />
                        <c:param name="category" value="${category}" />
                        <c:param name="q" value="${q}" />
                        <c:param name="page" value="${currentPage + 1}" />
                    </c:url>
                </c:if>
                <t:tableFooter itemLabel="고객사 히스토리"
                               currentPage="${currentPage}"
                               totalPages="${totalPages}"
                               previousUrl="${previousHistoryUrl}"
                               nextUrl="${nextHistoryUrl}"
                               paginationLabel="고객사 히스토리 페이지" />
            </c:when>
            <c:otherwise>
                <div class="customer-history-empty ui-empty-state">
                    <c:choose>
                        <c:when test="${hasActiveFilters}">
                            <i class="fas fa-search" aria-hidden="true"></i>
                            <strong>조건에 맞는 이력이 없습니다.</strong>
                            <span>검색 조건을 바꾸거나 초기화해 주세요.</span>
                            <a href="${pageContext.request.contextPath}/customer-history"
                               class="ui-button button--secondary button--md">검색 초기화</a>
                        </c:when>
                        <c:otherwise>
                            <i class="fas fa-history" aria-hidden="true"></i>
                            <strong>등록된 작업 이력이 없습니다.</strong>
                            <span>주요 장애, 업그레이드 또는 증설 작업부터 기록해 보세요.</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%@ include file="/includes/footer.jsp" %>
