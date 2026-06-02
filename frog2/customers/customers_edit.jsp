<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="고객사 정보 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<!-- 목록 페이지의 레이아웃 감각 적용: 폭/여백/타이포 일치 -->
<div class="customer-edit-page customer-management">
    <!-- 공통 페이지 헤더 컴포넌트 사용 -->
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-edit"></i> 고객사 정보 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            "<strong>${customer.customerName}</strong>" 고객사의 정보를 수정합니다.
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="${pageContext.request.contextPath}/customers?view=list" class="add-button">
                <i class="fas fa-list"></i> 목록으로
            </a>
            <a href="${pageContext.request.contextPath}/customers?view=detail&customerName=${customer.customerName}" class="add-button secondary">
                <i class="fas fa-info-circle"></i> 상세보기
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="alert alert-danger">
            <i class="fas fa-exclamation-circle"></i> ${error}
        </div>
    </c:if>

    <!-- 수정 폼: 목록 페이지의 카드 톤과 동일한 .form-container 사용 -->
    <div class="form-container">
        <form method="post" action="${pageContext.request.contextPath}/customers">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="customer_name" value="${customer.customerName}">

            <!-- 기본 정보 -->
            <div class="section-title">기본 정보</div>

            <div class="form-row">
                <div class="form-group">
                    <label for="customer_name_display">고객사명</label>
                    <input type="text" id="customer_name_display" value="${customer.customerName}" readonly 
                           class="bg-light text-muted">
                    <small class="form-text">고객사명은 수정할 수 없습니다.</small>
                </div>
                <div class="form-group">
                    <label for="first_introduction_year">도입년도</label>
                    <input type="number" id="first_introduction_year" name="first_introduction_year" 
                           value="${customer.firstIntroductionYear}" min="2000" max="2030">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="db_name">DB명</label>
                    <input type="text" id="db_name" name="db_name" value="${customer.dbName}">
                </div>
                <div class="form-group">
                    <label for="vertica_version">Vertica 버전</label>
                    <input type="text" id="vertica_version" name="vertica_version" value="${customer.verticaVersion}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="vertica_eos">EOS 일자</label>
                    <input type="date" id="vertica_eos" name="vertica_eos" value="${customer.verticaEos}">
                </div>
                <div class="form-group">
                    <label for="mode">모드</label>
                    <select id="mode" name="mode">
                        <option value="">선택하세요</option>
                        <option value="ENT" ${customer.mode == 'ENT' ? 'selected' : ''}>ENT</option>
                        <option value="EON" ${customer.mode == 'EON' ? 'selected' : ''}>EON</option>
                    </select>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="os">운영체제</label>
                    <input type="text" id="os" name="os" value="${customer.os}">
                </div>
                <div class="form-group">
                    <label for="nodes">노드 수</label>
                    <input type="number" id="nodes" name="nodes" value="${customer.nodes}" min="1">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="license_size">라이선스 크기</label>
                    <input type="text" id="license_size" name="license_size" value="${customer.licenseSize}">
                </div>
                <div class="form-group">
                    <label for="said">SAID</label>
                    <input type="text" id="said" name="said" value="${customer.said}">
                </div>
            </div>

            <!-- 담당자 정보 -->
            <div class="section-title">담당자 정보</div>

            <div class="form-row">
                <div class="form-group">
                    <label for="manager_name">담당자</label>
                    <input type="text" id="manager_name" name="manager_name" value="${customer.managerName}">
                </div>
                <div class="form-group">
                    <label for="sub_manager_name">부담당자</label>
                    <input type="text" id="sub_manager_name" name="sub_manager_name" value="${customer.subManagerName}">
                </div>
            </div>

            <!-- 시스템 구성 -->
            <div class="section-title">시스템 구성</div>

            <div class="form-row">
                <div class="form-group">
                    <label for="os_storage_config">스토리지 구성</label>
                    <textarea id="os_storage_config" name="os_storage_config" rows="3">${customer.osStorageConfig}</textarea>
                </div>
                <div class="form-group">
                    <label for="backup_config">백업 구성</label>
                    <textarea id="backup_config" name="backup_config" rows="3">${customer.backupConfig}</textarea>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="customer_type">고객 유형</label>
                    <select class="form-control" id="customer_type" name="customer_type">
                        <option value="">선택하세요</option>
                        <option value="정기점검 계약 고객사" ${customer.customerType == '정기점검 계약 고객사' ? 'selected' : ''}>정기점검 계약 고객사</option>
                        <option value="납품 계약 고객사" ${customer.customerType == '납품 계약 고객사' ? 'selected' : ''}>납품 계약 고객사</option>
                        <option value="유지보수 종료 고객사" ${customer.customerType == '유지보수 종료 고객사' ? 'selected' : ''}>유지보수 종료 고객사</option>
                    </select>
                </div>
                <div class="form-group">
                    <!-- 균형 맞춤용 빈 칸 -->
                </div>
            </div>

            <!-- 외부 솔루션 -->
            <div class="section-title">외부 솔루션</div>

            <div class="form-row">
                <div class="form-group">
                    <label for="etl_tool">ETL Tool</label>
                    <input type="text" id="etl_tool" name="etl_tool" value="${customer.etlTool}">
                </div>
                <div class="form-group">
                    <label for="bi_tool">BI Tool</label>
                    <input type="text" id="bi_tool" name="bi_tool" value="${customer.biTool}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="db_encryption">DB 암호화</label>
                    <input type="text" id="db_encryption" name="db_encryption" value="${customer.dbEncryption}">
                </div>
                <div class="form-group">
                    <label for="cdc_tool">CDC Tool</label>
                    <input type="text" id="cdc_tool" name="cdc_tool" value="${customer.cdcTool}">
                </div>
            </div>

            <!-- 비고 -->
            <div class="form-row">
                <div class="form-group full-width">
                    <label for="note">비고</label>
                    <textarea id="note" name="note" rows="3">${customer.note}</textarea>
                </div>
            </div>

            <!-- 버튼 -->
            <div class="button-group">
                <a href="${pageContext.request.contextPath}/customers?view=list" class="btn btn-cancel">취소</a>
                <a href="${pageContext.request.contextPath}/customers?view=detail&customerName=${customer.customerName}" class="btn btn-secondary">상세보기</a>
                <button type="submit" class="btn btn-primary">수정 완료</button>
            </div>
        </form>
    </div>
</div>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">

<script>
// 폼 유효성 검사 및 확인
document.querySelector('form').addEventListener('submit', function(e) {
    if (!confirm('고객사 정보를 수정하시겠습니까?')) {
        e.preventDefault();
        return false;
    }
});

// 페이지 로드 시 포커스
document.addEventListener('DOMContentLoaded', function() {
    // 목록 페이지와 동일한 본문 레이아웃 클래스 적용
    document.body.classList.add('page-1050');
    // 첫 번째 편집 가능한 필드에 포커스
    const firstInput = document.querySelector('#first_introduction_year');
    if (firstInput) {
        firstInput.focus();
    }
});
</script>

<%@ include file="/includes/footer.jsp" %>
