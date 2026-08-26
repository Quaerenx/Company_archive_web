<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="pageTitle" value="정기점검 이력 수정" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_calendar.js,/resources/js/pages/maintenance_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url value="/maintenance" var="customerHistoryUrl">
    <c:param name="view" value="history"/>
    <c:param name="customerName" value="${record.customerName}"/>
</c:url>

<div class="container maintenance-edit-page maintenance-form-page content-shell"
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
                        class="ui-button button--danger button--sm"
                        data-busy-label="삭제 중"><i class="fas fa-trash"></i> 삭제</button>
            </form>
            <c:url value="/maintenance" var="headerHistoryUrl">
                <c:param name="view" value="history"/>
                <c:param name="customerName" value="${record.customerName}"/>
            </c:url>
            <a href="${headerHistoryUrl}"
               class="ui-button button--secondary button--sm"><i class="fas fa-history"></i> 이력으로</a>
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

            <%@ include file="/WEB-INF/includes/maintenance_form_fields.jspf" %>
            
            <!-- 이력 정보 -->
            <div class="section-title">이력 정보</div>
            
            <div class="form-row">
                <div class="form-group">
                    <label for="maintenanceCreatedAt">등록일시</label>
                    <input type="text" id="maintenanceCreatedAt"
                           value="<fmt:formatDate value='${record.createdAt}' pattern='yyyy-MM-dd HH:mm:ss'/>"
                           readonly class="readonly-field">
                </div>
                <div class="form-group">
                    <label for="maintenanceUpdatedAt">수정일시</label>
                    <input type="text" id="maintenanceUpdatedAt"
                           value="<fmt:formatDate value='${record.updatedAt}' pattern='yyyy-MM-dd HH:mm:ss'/>"
                           readonly class="readonly-field">
                </div>
            </div>
            
            <!-- 버튼 -->
            <div class="button-group ui-form-actions">
                <c:url value="/maintenance" var="cancelUrl">
                    <c:param name="view" value="history"/>
                    <c:param name="customerName" value="${record.customerName}"/>
                </c:url>
                <a href="${cancelUrl}"
                   class="ui-button button--secondary button--md">취소</a>
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="수정 중">수정하기</button>
            </div>
        </form>
        
        
    </div>
</div>




<%@ include file="/includes/footer.jsp" %>
