<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="고객사 정보" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customers.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customers_list.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

<div class="customer-management content-management content-shell" data-customer-list data-context-path="<c:out value='${pageContext.request.contextPath}' />" data-filter="<c:out value='${filter}' />" data-sort-field="<c:out value='${sortField}' />" data-sort-direction="<c:out value='${sortDirection}' />" data-query="<c:out value='${q}' />" data-page-size="<c:out value='${pageSize}' />">
    <t:pageHeader>
        <jsp:attribute name="title">
            고객사 정보
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            고객사별 시스템 환경과 담당 정보를 확인하고 관리합니다.
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
    <div class="table-container customer-list-panel">
      <div class="customer-list-toolbar">
        <div class="filter-toggle">
            <button type="button" class="filter-btn ui-touch-target ${filter == 'maintenance' ? 'active' : ''} js-customer-filter" data-filter="maintenance">
                정기점검 <span class="filter-btn__count"><c:out value="${maintenanceCount}" /></span>
            </button>
            <button type="button" class="filter-btn ui-touch-target ${filter == 'all' ? 'active' : ''} js-customer-filter" data-filter="all">
                전체 <span class="filter-btn__count"><c:out value="${totalCount}" /></span>
            </button>
        </div>
        <form class="search-container ui-form"
              id="customer-search-form"
              method="get"
              action="${pageContext.request.contextPath}/customers"
              data-ui-submit-lock="auto">
            <input type="hidden" name="view" value="list" />
            <input type="hidden" name="filter" value="<c:out value='${filter}' />" />
            <input type="hidden" name="sortField" value="<c:out value='${sortField}' />" />
            <input type="hidden" name="sortDirection" value="<c:out value='${sortDirection}' />" />
            <input type="hidden" name="pageSize" value="<c:out value='${pageSize}' />" />
            <div class="search-input-wrapper ${not empty q ? 'has-query' : ''}">
                <label for="search-input" class="sr-only">고객사 검색</label>
                <i class="fas fa-search search-icon" aria-hidden="true"></i>
                <input type="text"
                       id="search-input"
                       name="q"
                       class="search-input"
                       value="<c:out value='${q}' />"
                       placeholder="고객사명, 버전, 모드, OS, SAID, 담당자 검색..."
                       minlength="2"
                       maxlength="100"
                       autocomplete="off">
                <button type="button" id="clear-search" class="clear-search ui-touch-target"
                        aria-label="검색어 지우기">
                    <i class="fas fa-times" aria-hidden="true"></i>
                </button>
            </div>
            <button type="submit"
                    class="ui-button button--secondary button--sm"
                    data-busy-label="검색 중">검색</button>
            <c:if test="${not empty q}">
                <span class="customer-search-result">
                    “<c:out value="${q}" />” 검색 결과 <strong><c:out value="${currentCount}" /></strong>개
                </span>
            </c:if>
        </form>
      </div>

    <c:set var="nextCustomerNameDirection" value="${sortField eq 'customer_name' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextVersionDirection" value="${sortField eq 'vertica_version' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextModeDirection" value="${sortField eq 'mode' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextOsDirection" value="${sortField eq 'os' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextNodesDirection" value="${sortField eq 'nodes' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextLicenseDirection" value="${sortField eq 'license_size' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextSaidDirection" value="${sortField eq 'said' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:set var="nextManagerDirection" value="${sortField eq 'manager_name' and sortDirection eq 'ASC' ? 'DESC' : 'ASC'}" />
    <c:url var="customerNameSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="customer_name"/><c:param name="sortDirection" value="${nextCustomerNameDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="versionSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="vertica_version"/><c:param name="sortDirection" value="${nextVersionDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="modeSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="mode"/><c:param name="sortDirection" value="${nextModeDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="osSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="os"/><c:param name="sortDirection" value="${nextOsDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="nodesSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="nodes"/><c:param name="sortDirection" value="${nextNodesDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="licenseSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="license_size"/><c:param name="sortDirection" value="${nextLicenseDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="saidSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="said"/><c:param name="sortDirection" value="${nextSaidDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>
    <c:url var="managerSortUrl" value="/customers"><c:param name="view" value="list"/><c:param name="filter" value="${filter}"/><c:param name="sortField" value="manager_name"/><c:param name="sortDirection" value="${nextManagerDirection}"/><c:param name="q" value="${q}"/><c:param name="pageSize" value="${pageSize}"/></c:url>

        <div class="table-wrapper ui-table-wrap"
             data-ui-scroll-region
             data-ui-scroll-label="고객사 정보 표">
            <table class="customer-table ui-table">
                <caption class="sr-only">고객사 정보 목록</caption>
                <thead>
                    <tr>
                        <th scope="col" aria-sort="${sortField eq 'customer_name' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${customerNameSortUrl}' />" class="js-customer-sort" data-sort-field="customer_name">
                                고객사
                                <i class="fas fa-sort sort-icon ${sortField == 'customer_name' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'vertica_version' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${versionSortUrl}' />" class="js-customer-sort" data-sort-field="vertica_version">
                                버전
                                <i class="fas fa-sort sort-icon ${sortField == 'vertica_version' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'mode' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${modeSortUrl}' />" class="js-customer-sort" data-sort-field="mode">
                                모드
                                <i class="fas fa-sort sort-icon ${sortField == 'mode' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'os' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${osSortUrl}' />" class="js-customer-sort" data-sort-field="os">
                                OS
                                <i class="fas fa-sort sort-icon ${sortField == 'os' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'nodes' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${nodesSortUrl}' />" class="js-customer-sort" data-sort-field="nodes">
                                노드수
                                <i class="fas fa-sort sort-icon ${sortField == 'nodes' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'license_size' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${licenseSortUrl}' />" class="js-customer-sort" data-sort-field="license_size">
                                라이선스
                                <i class="fas fa-sort sort-icon ${sortField == 'license_size' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'said' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${saidSortUrl}' />" class="js-customer-sort" data-sort-field="said">
                                SAID
                                <i class="fas fa-sort sort-icon ${sortField == 'said' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th scope="col" aria-sort="${sortField eq 'manager_name' ? (sortDirection eq 'ASC' ? 'ascending' : 'descending') : 'none'}">
                            <a href="<c:out value='${managerSortUrl}' />" class="js-customer-sort" data-sort-field="manager_name">
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
                            <c:param name="returnFilter" value="${filter}" />
                            <c:param name="returnSortField" value="${sortField}" />
                            <c:param name="returnSortDirection" value="${sortDirection}" />
                            <c:param name="returnQ" value="${q}" />
                            <c:param name="returnPage" value="${currentPage}" />
                            <c:param name="returnPageSize" value="${pageSize}" />
                        </c:url>
                        <tr class="customer-row"
                            data-detail-url="<c:out value="${customerDetailUrl}" />">
                            <td title="<c:out value="${customer.customerName}" />" data-original="<c:out value="${customer.customerName}" />">
                                <a class="customer-detail-link"
                                   href="<c:out value='${customerDetailUrl}' />"><c:out value="${customer.customerName}" default="" /></a>
                            </td>
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
                                        <c:when test="${not empty q}">
                                            검색 조건에 맞는 고객사가 없습니다.
                                        </c:when>
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

        </div>
        <c:if test="${currentPage > 1}">
            <c:url var="customerPreviousPageUrl" value="/customers">
                <c:param name="view" value="list" />
                <c:param name="filter" value="${filter}" />
                <c:param name="sortField" value="${sortField}" />
                <c:param name="sortDirection" value="${sortDirection}" />
                <c:param name="page" value="${currentPage - 1}" />
                <c:param name="pageSize" value="${pageSize}" />
                <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
            </c:url>
        </c:if>
        <c:if test="${currentPage < totalPages}">
            <c:url var="customerNextPageUrl" value="/customers">
                <c:param name="view" value="list" />
                <c:param name="filter" value="${filter}" />
                <c:param name="sortField" value="${sortField}" />
                <c:param name="sortDirection" value="${sortDirection}" />
                <c:param name="page" value="${currentPage + 1}" />
                <c:param name="pageSize" value="${pageSize}" />
                <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
            </c:url>
        </c:if>
        <t:tableFooter itemLabel="고객사"
                       currentPage="${currentPage}"
                       totalPages="${totalPages}"
                       previousUrl="${customerPreviousPageUrl}"
                       nextUrl="${customerNextPageUrl}"
                       paginationLabel="고객사 페이지" />
    </div>
    <div class="customer-secondary-action">
        <a href="${pageContext.request.contextPath}/customers?view=add"
           class="ui-button button--secondary button--sm">
            새 고객사 추가
        </a>
    </div>
  </div>

<%@ include file="/includes/footer.jsp" %>
