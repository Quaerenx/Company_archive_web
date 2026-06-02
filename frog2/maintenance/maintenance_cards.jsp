<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="정기점검 이력관리" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
<style>
	.maintenance-management {
	    width: 100%;
	    max-width: 1000px;
	    margin: 0 auto;
	    padding: var(--space-32) var(--space-16);
	    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	}
    
    .alert {
        padding: 1rem 1.25rem;
        margin-bottom: 1.5rem;
        border-radius: 8px;
        border: none;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    }
    
    .alert-success {
        background: #f0fdf4;
        color: #166534;
        border-left: 4px solid #22c55e;
    }
    
    .alert-danger {
        background: #fef2f2;
        color: #991b1b;
        border-left: 4px solid #ef4444;
    }
    
    /* 담당자별 섹션 */
    .inspector-block { background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 1rem; margin-bottom: 1rem; }
    .inspector-section { display: flex; gap: 1rem; align-items: flex-start; }
    .inspector-header { min-width: 180px; border-left: 3px solid var(--primary); padding-left: 0.75rem; color: #374151; font-weight: 600; font-size: 1rem; display: flex; align-items: center; gap: 0.5rem; background: transparent; box-shadow: none; border-radius: 0; }
    .inspector-header i { color: var(--primary); font-size: 1rem; }
    /* customer-count 제거 */
    
    /* 카드 그리드 */
    .customer-grid { flex: 1; display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 0.75rem; padding: 0; background: transparent; border: none; }
    
    /* 고객사 카드 */
    .customer-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 0.875rem; margin: 0; transition: background-color 0.2s, border-color 0.2s; cursor: pointer; position: relative; overflow: hidden; }
    
    .customer-card::before { display: none; }
    .customer-card:hover { background: #f8fafc; border-color: #e1e7ef; }
    
    .customer-name { font-size: 0.95rem; font-weight: 600; color: #1f2937; margin-bottom: 0.5rem; display: flex; align-items: center; gap: 0.5rem; }
    .customer-name i { color: var(--primary); font-size: 0.95rem; }
    
    .customer-info { display: grid; gap: 0.375rem; font-size: 0.85rem; color: #6b7280; }
    
    .info-row { display: flex; justify-content: space-between; align-items: center; }
    
    .info-label {
        font-weight: 500;
        color: #374151;
    }
    
    .info-value { color: #6b7280; text-align: right; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    
    .version-badge { background: #eef6ff; color: #1e40af; padding: 0.125rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 500; }
    .mode-badge { background: #f5fdf7; color: #166534; padding: 0.125rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 500; }
    
    /*.card-footer { margin-top: 0.5rem; padding-top: 0.5rem; border-top: 1px solid #f3f4f6; display: flex; justify-content: flex-end; }*/
    .view-history-btn { background: transparent; color: var(--primary); padding: 0; border: none; font-size: 0.85rem; }
    .view-history-btn:hover { text-decoration: underline; }
    
    .empty-state {
        text-align: center;
        padding: 3rem;
        color: #9ca3af;
        font-style: italic;
        background: white;
        border-radius: 12px;
        border: 2px dashed #d1d5db;
    }
    
    .empty-state i {
        font-size: 3rem;
        margin-bottom: 1rem;
        opacity: 0.5;
    }
    
    /* 반응형 디자인 */
	@media (max-width: 768px) {
	    .maintenance-management {
	        max-width: 100%;
	        padding: var(--space-24) var(--space-16);
	    }
        
        .customer-grid {
            grid-template-columns: 1fr;
            gap: 1rem;
            padding: 1rem;
        }
        
        .inspector-header {
            font-size: 1.1rem;
            padding: 0.875rem 1rem;
        }
        
        .customer-card {
            padding: 1rem;
        }
        
        .info-value {
            max-width: 120px;
        }
    }
    
    @media (max-width: 480px) {
        .customer-grid {
            padding: 0.75rem;
        }
        
        .customer-card {
            padding: 0.875rem;
        }
        
        .info-row {
            flex-direction: column;
            align-items: flex-start;
            gap: 0.25rem;
        }
        
        .info-value {
            max-width: none;
            text-align: left;
        }
        
    }
</style>

<div class="maintenance-management">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-clipboard-check"></i> 정기점검 이력관리</jsp:attribute>
        <jsp:attribute name="subtitle">담당자별 고객사를 선택하여 정기점검 이력을 관리하세요</jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/maintenance?view=add" class="add-button"><i class="fas fa-plus"></i> 이력 추가</a>
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
    
    <!-- 담당자별 고객사 카드 목록 -->
    <c:choose>
        <c:when test="${not empty inspectorCustomers}">
            <!-- 접속 사용자와 동일한 점검자 섹션을 우선 배치 -->
            <c:forEach var="entry" items="${inspectorCustomers}">
                <c:if test="${entry.key == user.userName}">
                    <div class="inspector-block">
                    <div class="inspector-section">
                        <div class="inspector-header">
                            <i class="fas fa-user-tie"></i>
                            <span>${entry.key}</span>
                        </div>
                        
                        <div class="customer-grid">
                            <c:forEach var="customer" items="${entry.value}">
                                <div class="customer-card" 
                                     onclick="location.href='${pageContext.request.contextPath}/maintenance?view=history&customerName=${customer.customerName}'">
                                    
                                    <div class="customer-name">
                                        <i class="fas fa-building"></i>
                                        ${customer.customerName}
                                    </div>
                                    
                                    <div class="customer-info">
                                        <div class="info-row">
                                            <span class="info-label">DB명</span>
                                            <span class="info-value" title="${customer.dbName}">${customer.dbName}</span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">버전</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.verticaVersion}">
                                                    <span class="version-badge">${customer.verticaVersion}</span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">모드</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.mode}">
                                                    <span class="mode-badge">${customer.mode}</span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">노드수</span>
                                            <span class="info-value">${customer.nodes}</span>
                                        </div>
                                    </div>
                                    
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    </div>
                </c:if>
            </c:forEach>

            <!-- 나머지 점검자 섹션들 -->
            <c:forEach var="entry" items="${inspectorCustomers}">
                <c:if test="${entry.key != user.userName}">
                    <div class="inspector-block">
                    <div class="inspector-section">
                        <div class="inspector-header">
                            <i class="fas fa-user-tie"></i>
                            <span>${entry.key}</span>
                        </div>
                        
                        <div class="customer-grid">
                            <c:forEach var="customer" items="${entry.value}">
                                <div class="customer-card" 
                                     onclick="location.href='${pageContext.request.contextPath}/maintenance?view=history&customerName=${customer.customerName}'">
                                    
                                    <div class="customer-name">
                                        <i class="fas fa-building"></i>
                                        ${customer.customerName}
                                    </div>
                                    
                                    <div class="customer-info">
                                        <div class="info-row">
                                            <span class="info-label">DB명</span>
                                            <span class="info-value" title="${customer.dbName}">${customer.dbName}</span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">버전</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.verticaVersion}">
                                                    <span class="version-badge">${customer.verticaVersion}</span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">모드</span>
                                            <span class="info-value">
                                                <c:if test="${not empty customer.mode}">
                                                    <span class="mode-badge">${customer.mode}</span>
                                                </c:if>
                                            </span>
                                        </div>
                                        <div class="info-row">
                                            <span class="info-label">노드수</span>
                                            <span class="info-value">${customer.nodes}</span>
                                        </div>
                                    </div>
                                    
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    </div>
                </c:if>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="empty-state">
                <i class="fas fa-users"></i>
                <div>등록된 고객사 정보가 없습니다.</div>
                <p>먼저 고객사 정보를 등록해주세요.</p>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    // 카드 호버 효과 강화
    const cards = document.querySelectorAll('.customer-card');
    
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-6px) scale(1.02)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });
    
    // 카드 클릭 시 로딩 효과
    cards.forEach(card => {
        card.addEventListener('click', function() {
            this.style.opacity = '0.7';
            this.style.transform = 'scale(0.98)';
        });
    });
});
</script>

<%@ include file="/includes/footer.jsp" %>
