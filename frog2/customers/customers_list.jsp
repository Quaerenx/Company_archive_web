<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="pageTitle" value="고객사 정보" scope="request" />

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main_style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/utilities.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/dashboard_box.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
</head>
<body class="page-1050 page-customers">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<div class="customer-management">
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
            <a href="${pageContext.request.contextPath}/customers?view=add" class="add-button">
                <i class="fas fa-plus"></i>
                새 고객사 추가
            </a>
        </jsp:attribute>
    </t:pageHeader>
	    
	<!-- 성공/에러 메시지 표시 -->
	<c:if test="${not empty sessionScope.message}">
	    <div class="alert alert-success">
	        <i class="fas fa-check-circle"></i>
	        ${sessionScope.message}
	    </div>
	    <c:remove var="message" scope="session" />
	</c:if>
	
	<c:if test="${not empty sessionScope.error}">
	    <div class="alert alert-danger">
	        <i class="fas fa-exclamation-circle"></i>
	        ${sessionScope.error}
	    </div>
	    <c:remove var="error" scope="session" />
	</c:if>
    
    <!-- 필터 섹션 -->
    <div class="filter-section">
        <div class="filter-toggle">
            <button class="filter-btn ${filter == 'maintenance' ? 'active' : ''}" 
                    onclick="changeFilter('maintenance')">
                <i class="fas fa-clipboard-check"></i>
                정기점검만 보기
            </button>
            <button class="filter-btn ${filter == 'all' ? 'active' : ''}" 
                    onclick="changeFilter('all')">
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
                <button type="button" id="clear-search" class="clear-search">
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
        <div class="table-wrapper">
            <table class="customer-table">
                <thead>
                    <tr>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('customer_name')">
                                고객사
                                <i class="fas fa-sort sort-icon ${sortField == 'customer_name' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('vertica_version')">
                                버전
                                <i class="fas fa-sort sort-icon ${sortField == 'vertica_version' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('mode')">
                                모드
                                <i class="fas fa-sort sort-icon ${sortField == 'mode' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('os')">
                                OS
                                <i class="fas fa-sort sort-icon ${sortField == 'os' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('nodes')">
                                노드수
                                <i class="fas fa-sort sort-icon ${sortField == 'nodes' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('license_size')">
                                라이선스
                                <i class="fas fa-sort sort-icon ${sortField == 'license_size' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('said')">
                                SAID
                                <i class="fas fa-sort sort-icon ${sortField == 'said' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        <th>
                            <a href="javascript:void(0)" onclick="sortTable('manager_name')">
                                담당자
                                <i class="fas fa-sort sort-icon ${sortField == 'manager_name' ? 'active' : ''}"></i>
                            </a>
                        </th>
                        
                    </tr>
                </thead>
                <tbody id="customer-table-body">
                    <c:forEach var="customer" items="${customerList}">
                        <tr class="customer-row" 
                            data-search-text="${customer.customerName} ${customer.verticaVersion} ${customer.mode} ${customer.os} ${customer.nodes} ${customer.licenseSize} ${customer.said} ${customer.managerName}"
                            onclick="viewDetail('${customer.customerName}')" style="cursor:pointer;">
                            <td title="${customer.customerName}" data-original="${not empty customer.customerName ? customer.customerName : ''}">${not empty customer.customerName ? customer.customerName : ''}</td>
                            <td data-original="${not empty customer.verticaVersion ? customer.verticaVersion : ''}">${not empty customer.verticaVersion ? customer.verticaVersion : ''}</td>
                            <td data-original="${not empty customer.mode ? customer.mode : ''}">${not empty customer.mode ? customer.mode : ''}</td>
                            <td data-original="${not empty customer.os ? customer.os : ''}">${not empty customer.os ? customer.os : ''}</td>
                            <td data-original="${not empty customer.nodes ? customer.nodes : ''}">${not empty customer.nodes ? customer.nodes : ''}</td>
                            <td data-original="${not empty customer.licenseSize ? customer.licenseSize : ''}">${not empty customer.licenseSize ? customer.licenseSize : ''}</td>
                            <td data-original="${not empty customer.said ? customer.said : ''}">${not empty customer.said ? customer.said : ''}</td>
                            <td title="${customer.managerName}" data-original="${not empty customer.managerName ? customer.managerName : ''}">${not empty customer.managerName ? customer.managerName : ''}</td>
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

<%@ include file="/WEB-INF/includes/footer_content.jspf" %>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
    // 전역 변수
    var currentFilter = "${filter}";
    var currentSortField = "${sortField}";
    var currentSortDirection = "${sortDirection}";
    
    // jQuery 준비 완료 시 실행
    $(document).ready(function() {
        if (currentSortField && currentSortDirection) {
            var activeIcon = $('.sort-icon.active');
            activeIcon.removeClass("fa-sort").addClass(currentSortDirection === "ASC" ? "fa-sort-up" : "fa-sort-down");
        }
        
        // 테이블 로딩 애니메이션
        $('.customer-table tbody tr').each(function(index) {
            $(this).css('animation-delay', (index * 0.05) + 's');
        });
        
        // 툴팁 기능 (긴 텍스트에 대한 hover 툴팁)
        $('.customer-table td[title]').hover(
            function() {
                if (this.offsetWidth < this.scrollWidth) {
                    $(this).attr('data-toggle', 'tooltip');
                }
            }
        );
        
        // 검색 기능 초기화
        initializeSearch();
    });
    
    // 간단한 검색 기능
    function initializeSearch() {
        var searchInput = document.getElementById('search-input');
        var clearButton = document.getElementById('clear-search');
        var searchCount = document.getElementById('search-count');
        var searchText = document.getElementById('search-text');
        
        if (!searchInput || !clearButton || !searchCount || !searchText) {
            return;
        }
        
        searchInput.addEventListener('input', function() {
            var searchTerm = this.value.toLowerCase().trim();
            var rows = document.querySelectorAll('.customer-row');
            var noResultsDiv = document.getElementById('no-results');
            var visibleCount = 0;
            
            for (var i = 0; i < rows.length; i++) {
                var row = rows[i];
                var searchText = row.getAttribute('data-search-text').toLowerCase();
                
                if (!searchTerm || searchText.includes(searchTerm)) {
                    row.classList.remove('hidden');
                    visibleCount++;
                } else {
                    row.classList.add('hidden');
                }
            }
            
            if (visibleCount === 0 && searchTerm) {
                noResultsDiv.classList.remove('d-none');
            } else {
                noResultsDiv.classList.add('d-none');
            }
            
            if (searchTerm) {
                clearButton.classList.remove('d-none');
                searchCount.textContent = visibleCount + '/' + rows.length;
                searchText.textContent = '검색 결과';
            } else {
                clearButton.classList.add('d-none');
                searchCount.textContent = '전체';
                searchText.textContent = '결과 표시 중';
            }
        });
        
        clearButton.addEventListener('click', function() {
            searchInput.value = '';
            searchInput.focus();
            searchInput.dispatchEvent(new Event('input'));
        });
    }
    
    // 필터 변경 함수
    function changeFilter(filter) {
        var url = '${pageContext.request.contextPath}/customers?view=list&filter=' + filter;
        if (currentSortField) {
            url += '&sortField=' + currentSortField + '&sortDirection=' + currentSortDirection;
        }
        window.location.href = url;
    }
    
    // 정렬 함수
    function sortTable(field) {
        var direction = 'ASC';
        if (currentSortField === field && currentSortDirection === 'ASC') {
            direction = 'DESC';
        }
        
        var url = '${pageContext.request.contextPath}/customers?view=list&filter=' + currentFilter + 
                  '&sortField=' + field + '&sortDirection=' + direction;
        window.location.href = url;
    }
    
    // 고객사 상세보기 페이지로 이동
    function viewDetail(customerName) {
        var encodedName = encodeURIComponent(customerName);
        window.location.href = '${pageContext.request.contextPath}/customers?view=detail&customerName=' + encodedName;
    }
    
    // (편집/삭제 기능은 상세 페이지로 이동) 
</script>

</body>
</html>