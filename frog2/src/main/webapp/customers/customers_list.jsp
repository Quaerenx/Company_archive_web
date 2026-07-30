<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="고객사 정보" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customers_list.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

<div class="customer-management content-shell" data-customer-list data-context-path="<c:out value='${pageContext.request.contextPath}' />" data-filter="<c:out value='${filter}' />" data-sort-field="<c:out value='${sortField}' />" data-sort-direction="<c:out value='${sortDirection}' />">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-building"></i> 고객사 정보
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            <c:choose>
                <c:when test="${filter == 'maintenance'}">
                    정기점검 고객사: <strong>${currentCount}</strong>개
                    <span class="text-muted">(전체: ${totalCount}개)</span>
                </c:when>
                <c:otherwise>
                    전체 고객사: <strong>${currentCount}</strong>개
                    <span class="text-muted">(정기점검: ${maintenanceCount}개)</span>
                </c:otherwise>
            </c:choose>
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/customers?view=add"
               class="add-button ui-button button--primary button--md">
                <i class="fas fa-plus"></i>
                새 고객사 추가
            </a>
        </jsp:attribute>
    </t:pageHeader>

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

    <!-- 필터 섹션 -->
    <div class="filter-section">
        <div class="filter-toggle">
            <button type="button" class="filter-btn ui-touch-target ${filter == 'maintenance' ? 'active' : ''} js-customer-filter" data-filter="maintenance">
                <i class="fas fa-clipboard-check"></i>
                정기점검만 보기
            </button>
            <button type="button" class="filter-btn ui-touch-target ${filter == 'all' ? 'active' : ''} js-customer-filter" data-filter="all">
                <i class="fas fa-list"></i>
                전체 보기
            </button>
        </div>
        <div class="filter-info">
            <i class="fas fa-info-circle"></i>
            <span class="filter-count">
                <c:choose>
                    <c:when test="${filter == 'maintenance'}">
                        정기점검 ${currentCount}개 표시 중
                    </c:when>
                    <c:otherwise>
                        전체 ${currentCount}개 표시 중
                    </c:otherwise>
                </c:choose>
            </span>
        </div>
    </div>

    <!-- 검색 섹션 -->
    <div class="search-section">
        <div class="search-container">
            <div class="search-input-wrapper">
                <i class="fas fa-search search-icon"></i>
                <input type="text"
                       id="search-input"
                       class="search-input"
                       placeholder="고객사명, 버전, OS, 담당자 등으로 검색..."
                       autocomplete="off">
                <button type="button" id="clear-search" class="clear-search ui-touch-target"
                        aria-label="검색어 지우기">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="search-stats">
                <i class="fas fa-filter"></i>
                <span id="search-count" class="search-count">전체</span>
                <span id="search-text">결과 표시 중</span>
            </div>
        </div>
    </div>

    <div class="table-container">
        <div class="table-wrapper ui-table-wrap">
            <table class="customer-table ui-table">
                <thead>
                    <tr>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="customer_name">
                                고객사
                                <i class="fas fa-sort sort-icon ${sortField == 'customer_name' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="vertica_version">
                                버전
                                <i class="fas fa-sort sort-icon ${sortField == 'vertica_version' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="mode">
                                모드
                                <i class="fas fa-sort sort-icon ${sortField == 'mode' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="os">
                                OS
                                <i class="fas fa-sort sort-icon ${sortField == 'os' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="nodes">
                                노드수
                                <i class="fas fa-sort sort-icon ${sortField == 'nodes' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="license_size">
                                라이선스
                                <i class="fas fa-sort sort-icon ${sortField == 'license_size' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="said">
                                SAID
                                <i class="fas fa-sort sort-icon ${sortField == 'said' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="#" class="js-customer-sort" data-sort-field="manager_name">
                                담당자
                                <i class="fas fa-sort sort-icon ${sortField == 'manager_name' ? 'active' : ''}"></i>
                            </a>
                        </th>

                    </tr>
                </thead>
                <tbody id="customer-table-body">
                    <c:forEach var="customer" items="${customerList}">
                        <c:url var="customerDetailUrl" value="/customers">
                            <c:param name="view" value="detail" />
                            <c:param name="customerName" value="${customer.customerName}" />
                        </c:url>
                        <tr class="customer-row"
                            data-search-text="<c:out value="${customer.customerName}" /> <c:out value="${customer.verticaVersion}" /> <c:out value="${customer.mode}" /> <c:out value="${customer.os}" /> <c:out value="${customer.nodes}" /> <c:out value="${customer.licenseSize}" /> <c:out value="${customer.said}" /> <c:out value="${customer.managerName}" />"
                            data-detail-url="<c:out value="${customerDetailUrl}" />"
                            role="link" tabindex="0">
                            <td title="<c:out value="${customer.customerName}" />" data-original="<c:out value="${customer.customerName}" />"><c:out value="${customer.customerName}" default="" /></td>
                            <td data-original="<c:out value="${customer.verticaVersion}" />"><c:out value="${customer.verticaVersion}" default="" /></td>
                            <td data-original="<c:out value="${customer.mode}" />"><c:out value="${customer.mode}" default="" /></td>
                            <td data-original="<c:out value="${customer.os}" />"><c:out value="${customer.os}" default="" /></td>
                            <td data-original="<c:out value="${customer.nodes}" />"><c:out value="${customer.nodes}" default="" /></td>
                            <td data-original="<c:out value="${customer.licenseSize}" />"><c:out value="${customer.licenseSize}" default="" /></td>
                            <td data-original="<c:out value="${customer.said}" />"><c:out value="${customer.said}" default="" /></td>
                            <td title="<c:out value="${customer.managerName}" />" data-original="<c:out value="${customer.managerName}" />"><c:out value="${customer.managerName}" default="" /></td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty customerList}">
                        <tr id="empty-state">
                            <td colspan="8" class="empty-state">
                                <i class="fas fa-inbox"></i>
                                <div>
                                    <c:choose>
                                        <c:when test="${filter == 'maintenance'}">
                                            등록된 정기점검 고객사가 없습니다.
                                        </c:when>
                                        <c:otherwise>
                                            등록된 고객사 정보가 없습니다.
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>

            <!-- 검색 결과 없음 메시지 -->
            <div id="no-results" class="no-results d-none">
                <i class="fas fa-search"></i>
                <h3>검색 결과가 없습니다</h3>
                <p>다른 검색어로 다시 시도해보세요.</p>
            </div>
        </div>
    </div>
  </div>

<%@ include file="/includes/footer.jsp" %>
