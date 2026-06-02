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

	/* --- [변경/추가된 CSS] 가독성 개선 스타일 --- */
	
	/* 1. 카드 UI 적용 */
	.detail-section {
	    background-color: #ffffff;
	    border: 1px solid #e5e7eb;
	    border-radius: 12px;
	    padding: 24px;
	    margin-bottom: 24px;
	    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
	}

	/* 2. 타이포그래피 계층 구조 강화 */
	.detail-section-title {
	    font-size: 18px;
	    font-weight: 600;
	    color: #111827;
	    margin-bottom: 20px;
	    padding-bottom: 12px;
	    border-bottom: 1px solid #f3f4f6;
	}

	.detail-section-title i {
	    color: var(--primary);
	    margin-right: 8px;
	}

	.detail-label {
	    display: block;
	    font-size: 13px;
	    font-weight: 500;
	    color: #6b7280;
	    margin-bottom: 4px;
	}

	.detail-value {
	    font-size: 15px;
	    font-weight: 500;
	    color: #1f2937;
	}

	.note-content {
	    padding: 12px;
	    background-color: #f9fafb;
	    border-radius: 6px;
	    font-size: 14px;
	    line-height: 1.6;
	    color: #374151;
	    white-space: pre-wrap; 
	}

	/* 3. 여백 및 정렬 개선 */
	.detail-grid {
	    display: grid;
	    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
	    gap: 24px;
	}	

	.detail-item.full-width {
	    grid-column: 1 / -1;
	}

	/* 탭 UI */
	.env-tabs { margin-top: 16px; }
	.tab-nav { display: flex; gap: 8px; border-bottom: 1px; margin: 0; padding-left: 8px; }
	.tab-btn { padding: 10px 14px; border: 1px solid #e5e7eb; border-bottom: none; background: #f9fafb; color: #374151; border-top-left-radius: 8px; border-top-right-radius: 8px; cursor: pointer; }
	.tab-btn.active { background: #ffffff; color: #111827; font-weight: 600; border-color: #d1d5db; }
	.tab-btn.disabled { opacity: 0.5; cursor: not-allowed; }
	.tab-panel { display: none; }
	.tab-panel.active { display: block; }
	/* 탭과 첫 섹션 사이 간격 최소화 */
	.env-tabs .tab-panel { margin-top: 0; padding-top: 0; }
	.env-tabs .tab-panel > .detail-section:first-child { margin-top: 0; }

	/* 탭 버튼이 카드 상단에 자연스럽게 붙도록 보더 겹침 처리 */
	.env-tabs .tab-nav { margin-bottom: 0; }
	 /*.env-tabs .tab-btn { border-bottom: 1px solid #e5e7eb; }*/
	.env-tabs .tab-btn.active { border-bottom-color: #ffffff; margin-bottom: -1px; }
	.env-tabs .tab-panel > .detail-section:first-child { border-top-left-radius: 0; border-top-right-radius: 0; }
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
    
    <c:set var="hasAnyDetail" value="${not empty customerDetail or not empty customerDetailStg or not empty customerDetailDev}" />
    <c:if test="${hasAnyDetail}">
        <div class="detail-container env-tabs">
            <div class="tab-nav">
                <button type="button" class="tab-btn" data-target="env-prod">운영</button>
                <button type="button" class="tab-btn" data-target="env-stg">스테이징</button>
                <button type="button" class="tab-btn" data-target="env-dev">개발</button>
            </div>
            <div class="tab-panel" id="env-prod">
        <c:if test="${not empty customerDetail}">
        <div class="detail-container">
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
                    <div class="detail-item full-width">
                        <span class="detail-label">백업비고</span>
                        <div class="detail-value note-content">${not empty customerDetail.backupNote ? customerDetail.backupNote : '-'}</div>
                    </div>
                </div>
            </div>
            
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
                    <div class="detail-item full-width">
                        <span class="detail-label">비고</span>
                        <div class="detail-value note-content">${not empty customerDetail.note ? customerDetail.note : '-'}</div>
                    </div>
                </div>
            </div>
        </div>
    </c:if>
    <c:if test="${empty customerDetail}">
        <div class="alert alert-light">운영 환경 데이터가 없습니다.</div>
    </c:if>
    </div>
    <div class="tab-panel" id="env-stg">
        <c:if test="${empty customerDetailStg}">
            <div class="alert alert-light">스테이징 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailStg}">
            <c:set var="detail" value="${customerDetailStg}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    <div class="tab-panel" id="env-dev">
        <c:if test="${empty customerDetailDev}">
            <div class="alert alert-light">개발 환경 데이터가 없습니다.</div>
        </c:if>
        <c:if test="${not empty customerDetailDev}">
            <c:set var="detail" value="${customerDetailDev}" />
            <%@ include file="/customers/_detail_sections.jspf" %>
        </c:if>
    </div>
    </div>
    </c:if>
    
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
    
    <c:if test="${empty customer and not hasAnyDetail}">
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

    <div class="detail-actions" style="display:flex; justify-content:flex-end; gap:8px; margin:12px 0 0 0;">
        <a href="javascript:void(0)" onclick="editCustomer('${currentCustomerName}', getActiveEnv())" class="btn-min primary">
            <i class="fas fa-edit"></i> 정보수정
        </a>
        <a href="javascript:void(0)" onclick="deleteCustomer('${currentCustomerName}')" class="btn-min danger">
            <i class="fas fa-trash"></i> 고객사 삭제
        </a>
    </div>
</div>

<script>
// 탭 전환 스크립트
document.addEventListener('DOMContentLoaded', function() {
	var tabs = document.querySelectorAll('.tab-btn');
	var panels = document.querySelectorAll('.tab-panel');

	function setActive(targetId) {
		panels.forEach(function(p){ p.classList.remove('active'); });
		tabs.forEach(function(t){ t.classList.remove('active'); });
		var target = document.getElementById(targetId);
		if (target) target.classList.add('active');
		var btn = document.querySelector('.tab-btn[data-target="' + targetId + '"]');
		if (btn) btn.classList.add('active');
	}

	// 초기 활성 탭 결정: URL env 우선, 없으면 운영 > 스테이징 > 개발 순
	var initial = 'env-prod';
	var params = new URLSearchParams(window.location.search);
	var envParam = params.get('env');
	if (envParam === 'stg') initial = 'env-stg';
	if (envParam === 'dev') initial = 'env-dev';
	var prodEmpty = document.querySelector('#env-prod .alert');
	var stgEmpty = document.querySelector('#env-stg .alert');
	var devEmpty = document.querySelector('#env-dev .alert');
	if (prodEmpty) {
		if (!stgEmpty) initial = 'env-stg';
		else if (!devEmpty) initial = 'env-dev';
	}
	setActive(initial);

	tabs.forEach(function(tab){
		tab.addEventListener('click', function(){
			setActive(tab.getAttribute('data-target'));
		});
	});
});
function getActiveEnv() {
    var active = document.querySelector('.tab-btn.active');
    if (!active) return 'prod';
    var target = active.getAttribute('data-target');
    if (target === 'env-stg') return 'stg';
    if (target === 'env-dev') return 'dev';
    return 'prod';
}
function editCustomer(customerName, env) {
    var encodedName = encodeURIComponent(customerName);
    var url = '${pageContext.request.contextPath}/customers?view=editDetail&customerName=' + encodedName;
    if (env) url += '&env=' + encodeURIComponent(env);
    window.location.href = url;
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
