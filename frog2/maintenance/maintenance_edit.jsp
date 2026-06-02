<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="정기점검 이력 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">

<div class="container">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-edit"></i> 정기점검 이력 수정</jsp:attribute>
        <jsp:attribute name="subtitle">정기점검 이력 정보를 수정해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <form id="deleteFormHeader" method="post" action="${pageContext.request.contextPath}/maintenance" style="display:inline; margin-right:8px;" onsubmit="return confirm('정말 삭제하시겠습니까?');">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="maintenance_id" value="${record.maintenanceId}">
                <input type="hidden" name="customer_name" value="<c:out value='${record.customerName}'/>">
                <button type="submit" class="btn-min danger"><i class="fas fa-trash"></i> 삭제</button>
            </form>
            <c:url value="/maintenance" var="headerHistoryUrl">
                <c:param name="view" value="history"/>
                <c:param name="customerName" value="${record.customerName}"/>
            </c:url>
            <a href="${headerHistoryUrl}" class="btn-min"><i class="fas fa-history"></i> 이력으로</a>
        </jsp:attribute>
    </t:pageHeader>
    
    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
        </div>
    </c:if>
    
    <!-- 수정 폼 -->
    <div class="form-container">
        <form method="post" action="${pageContext.request.contextPath}/maintenance">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="maintenance_id" value="${record.maintenanceId}">
            <input type="hidden" id="current_customer_value" value="<c:out value='${record.customerName}'/>">
            <input type="hidden" id="current_inspector_value" value="<c:out value='${record.inspectorName}'/>">
            
            <!-- 기본 정보 -->
            <div class="section-title">기본 정보</div>
            
            <div class="form-row">
                <div class="form-group">
                    <label for="customer_name">고객사명 <span class="required">*</span></label>
                    <select id="customer_name" name="customer_name" required>
                        <option value="">고객사를 선택하세요</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="inspector_name">점검자 <span class="required">*</span></label>
                    <select id="inspector_name" name="inspector_name" required>
                        <option value="">점검자를 선택하세요</option>
                    </select>
                </div>
            </div>
            
            <div class="form-row">
                <div class="form-group">
                    <label for="inspection_date">점검일자 <span class="required">*</span></label>
                    <input type="date" id="inspection_date" name="inspection_date" 
                           value="<fmt:formatDate value='${record.inspectionDate}' pattern='yyyy-MM-dd'/>" required>
                </div>
                <div class="form-group">
                    <label for="vertica_version">Vertica 버전</label>
                    <input type="text" id="vertica_version" name="vertica_version" 
                           value="<c:out value='${record.verticaVersion}'/>" placeholder="예: 12.0.4">
                </div>
            </div>
            
            <!-- 라이선스 정보 -->
            <div class="section-title">라이선스 정보 (선택)</div>
            <div class="form-row">
                <div class="form-group">
                    <label for="license_size_gb">라이선스 크기 (TB)</label>
                    <!-- varchar(50)로 변경됨: 자유 입력, 최대 50자 -->
                    <input type="text" id="license_size_gb" name="license_size_gb" maxlength="50" value="<c:out value='${record.licenseSizeGb}'/>" placeholder="예: 4 또는 4TB">
                </div>
                <div class="form-group">
                    <label for="license_usage_size">라이선스 사용량 (TB)</label>
                    <input type="text" id="license_usage_size" name="license_usage_size" maxlength="50" value="<c:out value='${record.licenseUsageSize}'/>" placeholder="예: 3.5 또는 3.5TB">
                </div>
                <div class="form-group">
                    <label for="license_usage_pct">라이선스 사용률 (%)</label>
                    <input type="text" id="license_usage_pct" name="license_usage_pct" maxlength="50" value="<c:out value='${record.licenseUsagePct}'/>" placeholder="예: 75 또는 75%">
                </div>
            </div>
            
            <!-- 점검 내용 -->
            <div class="section-title">점검 내용</div>
            
            <div class="form-row">
                <div class="form-group full-width">
                    <label for="note">비고 및 점검 내용</label>
                    <textarea id="note" name="note" rows="8" 
                              placeholder="점검 내용, 발견된 이슈, 조치사항 등을 입력해주세요."><c:out value="${record.note}" /></textarea>
                </div>
            </div>
            
            <!-- 이력 정보 -->
            <div class="section-title">이력 정보</div>
            
            <div class="form-row">
                <div class="form-group">
                    <label>등록일시</label>
                    <input type="text" value="<fmt:formatDate value='${record.createdAt}' pattern='yyyy-MM-dd HH:mm:ss'/>" 
                           readonly class="readonly-field">
                </div>
                <div class="form-group">
                    <label>수정일시</label>
                    <input type="text" value="<fmt:formatDate value='${record.updatedAt}' pattern='yyyy-MM-dd HH:mm:ss'/>" 
                           readonly class="readonly-field">
                </div>
            </div>
            
            <!-- 버튼 -->
            <div class="button-group">
                <c:url value="/maintenance" var="cancelUrl">
                    <c:param name="view" value="history"/>
                    <c:param name="customerName" value="${record.customerName}"/>
                </c:url>
                <a href="${cancelUrl}" class="btn btn-cancel">취소</a>
                <button type="submit" class="btn btn-primary">수정하기</button>
            </div>
        </form>
        
        
    </div>
</div>



<script>
// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    // 고객사 및 담당자 목록 로드
    loadCustomersAndInspectors();
});

// 고객사 및 담당자 목록 로드
function loadCustomersAndInspectors() {
    // 기존 customers 테이블에서 고객사 및 담당자 정보 가져오기
    fetch('${pageContext.request.contextPath}/customers?action=getCustomersForMaintenance')
        .then(response => response.json())
        .then(data => {
            populateCustomers(data.customers);
            populateInspectors(data.inspectors);
            
            // 기존 값 설정
            setCurrentValues();
        })
        .catch(error => {
            console.error('Error:', error);
            // 에러 시 기본 옵션들 수동으로 추가
            addDefaultOptions();
            setCurrentValues();
        });
}

function getCurrentCustomer() {
    const input = document.getElementById('current_customer_value');
    return input ? input.value : '';
}

function getCurrentInspector() {
    const input = document.getElementById('current_inspector_value');
    return input ? input.value : '';
}

// 고객사 선택박스 채우기
function populateCustomers(customers) {
    const customerSelect = document.getElementById('customer_name');
    customers.forEach(customer => {
        const option = document.createElement('option');
        option.value = customer;
        option.textContent = customer;
        customerSelect.appendChild(option);
    });
}

// 점검자 선택박스 채우기
function populateInspectors(inspectors) {
    const inspectorSelect = document.getElementById('inspector_name');
    inspectors.forEach(inspector => {
        const option = document.createElement('option');
        option.value = inspector;
        option.textContent = inspector;
        inspectorSelect.appendChild(option);
    });
}

// 기본 옵션들 추가 (API 실패 시)
function addDefaultOptions() {
    const customerSelect = document.getElementById('customer_name');
    const inspectorSelect = document.getElementById('inspector_name');
    
    // 현재 값들을 기본 옵션으로 추가
    const currentCustomer = getCurrentCustomer();
    const currentInspector = getCurrentInspector();
    
    if (currentCustomer) {
        const option = document.createElement('option');
        option.value = currentCustomer;
        option.textContent = currentCustomer;
        customerSelect.appendChild(option);
    }
    
    if (currentInspector) {
        const option = document.createElement('option');
        option.value = currentInspector;
        option.textContent = currentInspector;
        inspectorSelect.appendChild(option);
    }
}

// 현재 값들 설정
function setCurrentValues() {
    const currentCustomer = getCurrentCustomer();
    const currentInspector = getCurrentInspector();
    
    if (currentCustomer) {
        document.getElementById('customer_name').value = currentCustomer;
    }
    
    if (currentInspector) {
        document.getElementById('inspector_name').value = currentInspector;
    }
}

// 삭제 확인
function confirmDelete() {
    if (confirm('정말로 이 정기점검 이력을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.')) {
        document.getElementById('deleteForm').submit();
    }
}

// 폼 유효성 검사
document.querySelector('form').addEventListener('submit', function(e) {
    if (e.target.id === 'deleteForm') return; // 삭제 폼은 검사하지 않음
    
    const customerNameEl = document.getElementById('customer_name');
    const inspectorNameEl = document.getElementById('inspector_name');
    const inspectionDateEl = document.getElementById('inspection_date');
    const sizeEl = document.getElementById('license_size_gb');
    const usageSizeEl = document.getElementById('license_usage_size');
    const usageEl = document.getElementById('license_usage_pct');

    const customerName = (customerNameEl.value || '').trim();
    const inspectorName = (inspectorNameEl.value || '').trim();
    const inspectionDate = inspectionDateEl.value;

    if (!customerName) {
        e.preventDefault();
        alert('고객사명을 선택해주세요.');
        customerNameEl.focus();
        return false;
    }
    
    if (!inspectorName) {
        e.preventDefault();
        alert('점검자를 선택해주세요.');
        inspectorNameEl.focus();
        return false;
    }
    
    if (!inspectionDate) {
        e.preventDefault();
        alert('점검일자를 입력해주세요.');
        inspectionDateEl.focus();
        return false;
    }

    // 선택 항목 길이 검증 (varchar(50))
    const sizeVal = (sizeEl.value || '').trim();
    const usageSizeVal = (usageSizeEl.value || '').trim();
    const usageVal = (usageEl.value || '').trim();

    if (sizeVal && sizeVal.length > 50) {
        e.preventDefault();
        alert('라이선스 크기는 최대 50자까지 입력할 수 있습니다.');
        sizeEl.focus();
        return false;
    }

    if (usageSizeVal && usageSizeVal.length > 50) {
        e.preventDefault();
        alert('라이선스 사용량은 최대 50자까지 입력할 수 있습니다.');
        usageSizeEl.focus();
        return false;
    }

    if (usageVal && usageVal.length > 50) {
        e.preventDefault();
        alert('라이선스 사용률은 최대 50자까지 입력할 수 있습니다.');
        usageEl.focus();
        return false;
    }
});
</script>

<%@ include file="/includes/footer.jsp" %>
