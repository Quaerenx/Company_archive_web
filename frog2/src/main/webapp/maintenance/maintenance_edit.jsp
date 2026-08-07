<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="정기점검 이력 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance_edit.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>


<div class="container maintenance-edit-page content-shell"
     data-maintenance-form-mode="edit"
     data-context-path="<c:out value='${pageContext.request.contextPath}' />">
    <t:pageHeader>
        <jsp:attribute name="title"><i class="fas fa-edit"></i> 정기점검 이력 수정</jsp:attribute>
        <jsp:attribute name="subtitle">정기점검 이력 정보를 수정해주세요.</jsp:attribute>
        <jsp:attribute name="actions">
            <form id="deleteFormHeader"
                  class="maintenance-delete-form ui-form"
                  method="post"
                  action="${pageContext.request.contextPath}/maintenance"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="maintenance_id" value="${record.maintenanceId}">
                <input type="hidden" name="customer_name" value="<c:out value='${record.customerName}'/>">
                <button type="submit"
                        class="btn-min danger ui-button button--danger button--sm"
                        data-busy-label="삭제 중"><i class="fas fa-trash"></i> 삭제</button>
            </form>
            <c:url value="/maintenance" var="headerHistoryUrl">
                <c:param name="view" value="history"/>
                <c:param name="customerName" value="${record.customerName}"/>
            </c:url>
            <a href="${headerHistoryUrl}"
               class="btn-min ui-button button--secondary button--sm"><i class="fas fa-history"></i> 이력으로</a>
        </jsp:attribute>
    </t:pageHeader>
    
    <!-- 오류 메시지 -->
    <c:if test="${not empty error}">
        <div class="alert alert-danger ui-alert ui-alert--danger"
             role="alert"
             aria-atomic="true">
            <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
        </div>
    </c:if>
    
    <!-- 수정 폼 -->
    <div class="form-container ui-form-card">
        <form id="maintenanceForm"
              class="ui-form ui-form-layout ui-form-layout--actions-end"
              method="post"
              action="${pageContext.request.contextPath}/maintenance"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="maintenance_id" value="${record.maintenanceId}">
            <input type="hidden" id="current_customer_value" value="<c:out value='${record.customerName}'/>">
            <input type="hidden" id="current_inspector_value" value="<c:out value='${record.inspectorName}'/>">

            <div id="maintenanceOptionsStatus"
                 class="ui-alert ui-alert--danger"
                 role="alert"
                 aria-atomic="true"
                 hidden>
                <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
                <span id="maintenanceOptionsStatusMessage"></span>
                <button type="button"
                        id="retryMaintenanceOptions"
                        class="ui-button button--secondary button--sm">다시 불러오기</button>
            </div>
            
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
                <a href="${cancelUrl}"
                   class="btn btn-cancel ui-button button--secondary button--md">취소</a>
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        data-busy-label="수정 중">수정하기</button>
            </div>
        </form>
        
        
    </div>
</div>




<%@ include file="/includes/footer.jsp" %>
