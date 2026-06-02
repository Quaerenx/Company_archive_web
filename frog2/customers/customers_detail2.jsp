<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="고객사 상세정보" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
<style>
	.customer-detail .header-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
</style>

<c:set var="currentCustomerName" value="${not empty customerDetail.customerName ? customerDetail.customerName : (not empty customer.customerName ? customer.customerName : '')}" />

<div class="customer-detail customer-management">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-building"></i>
            <c:choose>
                <c:when test="${not empty customerDetail.customerName}">
                    ${customerDetail.customerName}
                </c:when>
                <c:when test="${not empty customer.customerName}">
                    ${customer.customerName}
                </c:when>
                <c:otherwise>
                    고객사
                </c:otherwise>
            </c:choose>
            상세정보
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            고객사 정보 및 시스템 세부사항
        </jsp:attribute>
		<jsp:attribute name="actions">
			<div class="header-actions">
				<a href="${pageContext.request.contextPath}/customers?view=list" class="btn-min">
					<i class="fas fa-arrow-left"></i> 목록으로
				</a>
			</div>
		</jsp:attribute>
        <jsp:attribute name="extra">
            <c:if test="${not empty verticaEosDate}">
                <div class="alert alert-warning" style="margin-top:8px;">
                    <i class="fas fa-exclamation-triangle"></i>
                    Vertica EOS 일자: <strong><fmt:formatDate value="${verticaEosDate}" pattern="yyyy-MM-dd"/></strong>
                </div>
            </c:if>
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
        <!-- detail-container 아래 전역 액션 버튼 -->
        <!-- 이동: 모든 케이스 공통 영역으로 아래로 이동 -->
        <c:remove var="error" scope="session" />
    </c:if>
    
    <!-- 고객사 상세정보가 있는 경우 -->
    <c:if test="${not empty customerDetail}">
        <div class="detail-container">
            <!-- 메타정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-info-circle"></i>
                    메타정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">고객사</span>
                        <span class="detail-value">${not empty customerDetail.customerName ? customerDetail.customerName : '-'}</span>
                    </div>
                    <div class="detail-item">
					    <span class="detail-label">시스템명</span>
					    <span class="detail-value">${not empty customerDetail.systemName ? customerDetail.systemName : '-'}</span>
					</div>
                    <div class="detail-item">
                        <span class="detail-label">고객사 담당자</span>
                        <span class="detail-value">${not empty customerDetail.customerManager ? customerDetail.customerManager : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당 SI</span>
                        <span class="detail-value">${not empty customerDetail.siCompany ? customerDetail.siCompany : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">SI 담당자</span>
                        <span class="detail-value">${not empty customerDetail.siManager ? customerDetail.siManager : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">작성자</span>
                        <span class="detail-value">${not empty customerDetail.creator ? customerDetail.creator : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">작성일자</span>
                        <span class="detail-value">
                            <c:choose>
                                <c:when test="${not empty customerDetail.createDate}">
                                    <fmt:formatDate value="${customerDetail.createDate}" pattern="yyyy-MM-dd" />
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당자 정</span>
                        <span class="detail-value">${not empty customerDetail.mainManager ? customerDetail.mainManager : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">담당자 부</span>
                        <span class="detail-value">${not empty customerDetail.subManager ? customerDetail.subManager : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">설치일자</span>
                        <span class="detail-value">
                            <c:choose>
                                <c:when test="${not empty customerDetail.installDate}">
                                    <fmt:formatDate value="${customerDetail.installDate}" pattern="yyyy-MM-dd" />
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">도입년도</span>
                        <span class="detail-value">${not empty customerDetail.introductionYear ? customerDetail.introductionYear : '-'}</span>
                    </div>
                </div>
            </div>
            
            <!-- Vertica 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-database"></i>
                    Vertica 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">DB명</span>
                        <span class="detail-value">${not empty customerDetail.dbName ? customerDetail.dbName : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">DB mode</span>
                        <span class="detail-value">${not empty customerDetail.dbMode ? customerDetail.dbMode : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Version</span>
                        <span class="detail-value">${not empty customerDetail.verticaVersion ? customerDetail.verticaVersion : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">라이센스</span>
                        <span class="detail-value">${not empty customerDetail.licenseInfo ? customerDetail.licenseInfo : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">SAID</span>
                        <span class="detail-value">${not empty customerDetail.said ? customerDetail.said : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">노드 수</span>
                        <span class="detail-value">${not empty customerDetail.nodeCount ? customerDetail.nodeCount : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Vertica admin</span>
                        <span class="detail-value">${not empty customerDetail.verticaAdmin ? customerDetail.verticaAdmin : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Subcluster 유무</span>
                        <span class="detail-value">${not empty customerDetail.subclusterYn ? customerDetail.subclusterYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC 여부</span>
                        <span class="detail-value">${not empty customerDetail.mcYn ? customerDetail.mcYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC host</span>
                        <span class="detail-value">${not empty customerDetail.mcHost ? customerDetail.mcHost : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC version</span>
                        <span class="detail-value">${not empty customerDetail.mcVersion ? customerDetail.mcVersion : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">MC admin</span>
                        <span class="detail-value">${not empty customerDetail.mcAdmin ? customerDetail.mcAdmin : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">백업 여부</span>
                        <span class="detail-value">${not empty customerDetail.backupYn ? customerDetail.backupYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">사용자 정의 리소스풀 여부</span>
                        <span class="detail-value">${not empty customerDetail.customResourcePoolYn ? customerDetail.customResourcePoolYn : '-'}</span>
                    </div>
                </div>
                <div class="detail-item full-width mt-4">
                    <span class="detail-label">백업비고</span>
                    <div class="detail-value note-content">${not empty customerDetail.backupNote ? customerDetail.backupNote : '-'}</div>
                </div>
            </div>
            
            <!-- 환경 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-server"></i>
                    환경 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">OS</span>
                        <span class="detail-value">${not empty customerDetail.osInfo ? customerDetail.osInfo : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">메모리</span>
                        <span class="detail-value">${not empty customerDetail.memoryInfo ? customerDetail.memoryInfo : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">인프라 구분</span>
                        <span class="detail-value">${not empty customerDetail.infraType ? customerDetail.infraType : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CPU 소켓</span>
                        <span class="detail-value">${not empty customerDetail.cpuSocket ? customerDetail.cpuSocket : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">HyperThreading</span>
                        <span class="detail-value">${not empty customerDetail.hyperThreading ? customerDetail.hyperThreading : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CPU 코어</span>
                        <span class="detail-value">${not empty customerDetail.cpuCore ? customerDetail.cpuCore : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">/data 영역</span>
                        <span class="detail-value">${not empty customerDetail.dataArea ? customerDetail.dataArea : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Depot 영역</span>
                        <span class="detail-value">${not empty customerDetail.depotArea ? customerDetail.depotArea : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">/catalog 영역</span>
                        <span class="detail-value">${not empty customerDetail.catalogArea ? customerDetail.catalogArea : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">object 영역</span>
                        <span class="detail-value">${not empty customerDetail.objectArea ? customerDetail.objectArea : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Public 여부</span>
                        <span class="detail-value">${not empty customerDetail.publicYn ? customerDetail.publicYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Public 대역</span>
                        <span class="detail-value">${not empty customerDetail.publicNetwork ? customerDetail.publicNetwork : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Private 여부</span>
                        <span class="detail-value">${not empty customerDetail.privateYn ? customerDetail.privateYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Private 대역</span>
                        <span class="detail-value">${not empty customerDetail.privateNetwork ? customerDetail.privateNetwork : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">storage 여부</span>
                        <span class="detail-value">${not empty customerDetail.storageYn ? customerDetail.storageYn : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Storage 대역</span>
                        <span class="detail-value">${not empty customerDetail.storageNetwork ? customerDetail.storageNetwork : '-'}</span>
                    </div>
                </div>
            </div>
            
            <!-- 외부 솔루션 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-puzzle-piece"></i>
                    외부 솔루션
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">ETL</span>
                        <span class="detail-value">${not empty customerDetail.etlTool ? customerDetail.etlTool : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">BI</span>
                        <span class="detail-value">${not empty customerDetail.biTool ? customerDetail.biTool : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">DB암호화</span>
                        <span class="detail-value">${not empty customerDetail.dbEncryption ? customerDetail.dbEncryption : '-'}</span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">CDC</span>
                        <span class="detail-value">${not empty customerDetail.cdcTool ? customerDetail.cdcTool : '-'}</span>
                    </div>
                </div>
            </div>
            
            <!-- 기타 정보 섹션 -->
            <div class="detail-section">
                <div class="detail-section-title">
                    <i class="fas fa-sticky-note"></i>
                    기타 정보
                </div>
                <div class="detail-grid">
                    <div class="detail-item">
                        <span class="detail-label">EOS 일자</span>
                        <span class="detail-value">
                            <c:choose>
                                <c:when test="${not empty customerDetail.eosDate}">
                                    <fmt:formatDate value="${customerDetail.eosDate}" pattern="yyyy-MM-dd" />
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">고객 유형</span>
                        <span class="detail-value">${not empty customerDetail.customerType ? customerDetail.customerType : '-'}</span>
                    </div>
                </div>
                <div class="detail-item full-width mt-4">
                    <span class="detail-label">비고</span>
                    <div class="detail-value note-content">${not empty customerDetail.note ? customerDetail.note : '-'}</div>
                </div>
            </div>
        </div>
    </c:if>
    
    <!-- 상세정보가 없지만 기본 정보가 있는 경우 -->
    <c:if test="${empty customerDetail and not empty customer}">
        <div class="detail-container">
            <div class="detail-section text-center p-5">
                <i class="fas fa-info-circle text-warning" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <h3 class="text-dark mb-3">상세정보가 등록되지 않았습니다</h3>
                <p class="text-muted mb-4">
                    ${customer.customerName}의 기본 정보는 있지만 상세정보가 등록되지 않았습니다.<br>
                    상세정보를 등록하려면 수정 페이지에서 추가해 주세요.
                </p>
                <div class="d-flex gap-3 justify-content-center">
                    <a href="${pageContext.request.contextPath}/customers?view=list" class="btn btn-secondary">
                        <i class="fas fa-arrow-left"></i>
                        목록으로 돌아가기
                    </a>
				<a href="${pageContext.request.contextPath}/customers?view=editDetail&customerName=${customer.customerName}" class="btn btn-primary">
				    <i class="fas fa-edit"></i>
				    정보 수정하기
				</a>
                </div>
            </div>
        </div>
    </c:if>
    
    <!-- 고객사 정보가 전혀 없는 경우 -->
    <c:if test="${empty customer and empty customerDetail}">
        <div class="detail-container">
            <div class="detail-section text-center p-5">
                <i class="fas fa-exclamation-triangle text-danger" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <h3 class="text-dark mb-3">고객사 정보를 찾을 수 없습니다</h3>
                <p class="text-muted mb-4">요청하신 고객사 정보가 존재하지 않거나 삭제되었을 수 있습니다.</p>
                <a href="${pageContext.request.contextPath}/customers?view=list" class="btn btn-primary">
                    <i class="fas fa-arrow-left"></i>
                    목록으로 돌아가기
                </a>
            </div>
        </div>
    </c:if>
<!-- detail-container 외부 공통 액션 영역: 언제나 표시 -->
<div class="detail-actions" style="display:flex; justify-content:flex-end; gap:8px; margin:12px 0 0 0;">
    <a href="javascript:void(0)" onclick="editCustomer('${currentCustomerName}')" class="btn-min primary">
        <i class="fas fa-edit"></i> 정보수정
    </a>
    <a href="javascript:void(0)" onclick="deleteCustomer('${currentCustomerName}')" class="btn-min danger">
        <i class="fas fa-trash"></i> 고객사 삭제
    </a>
    
</div>
</div>

<script>
function editCustomer(customerName) {
	var encodedName = encodeURIComponent(customerName);
	window.location.href = '${pageContext.request.contextPath}/customers?view=editDetail&customerName=' + encodedName;
}

function deleteCustomer(customerName) {
	if (confirm('정말로 "' + customerName + '" 고객사를 삭제하시겠습니까?\n\n삭제된 데이터는 복구할 수 없습니다.')) {
		var form = document.createElement('form');
		form.method = 'POST';
		form.action = '${pageContext.request.contextPath}/customers';

		var actionInput = document.createElement('input');
		actionInput.type = 'hidden';
		actionInput.name = 'action';
		actionInput.value = 'delete';

		var nameInput = document.createElement('input');
		nameInput.type = 'hidden';
		nameInput.name = 'customer_name';
		nameInput.value = customerName;

		form.appendChild(actionInput);
		form.appendChild(nameInput);
		document.body.appendChild(form);
		form.submit();
	}
}
</script>

<%@ include file="/includes/footer.jsp" %>
