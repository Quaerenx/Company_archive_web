<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="pageTitle" value="정기점검 이력 추가" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-maintenance" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/maintenance.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/maintenance_calendar.js,/resources/js/pages/maintenance_form.js" scope="request" />
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ include file="/includes/header.jsp" %>

<c:url var="customerHistoryUrl" value="/maintenance">
    <c:param name="view" value="history" />
    <c:param name="customerName" value="${customerName}" />
</c:url>

<!-- 전체를 maintenance-add-page 클래스로 감싸기 -->
<div class="maintenance-add-page maintenance-form-page"
     data-maintenance-form-mode="add"
     data-context-path="<c:out value='${pageContext.request.contextPath}' />">
    <div class="container content-shell">
        <t:pageHeader>
            <jsp:attribute name="title"><i class="fas fa-plus-circle"></i> 새 정기점검 이력 등록</jsp:attribute>
            <jsp:attribute name="subtitle">
                <c:if test="${not empty customerName}"><strong><c:out value="${customerName}" /></strong>의 정기점검 이력을 입력해주세요.</c:if>
            </jsp:attribute>
            <jsp:attribute name="actions">
                <c:choose>
                    <c:when test="${not empty customerName}">
                        <a href="<c:out value='${customerHistoryUrl}' />"
                           class="ui-button button--secondary button--md"><i class="fas fa-history"></i> 이력으로</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/maintenance?view=cards"
                           class="ui-button button--secondary button--md"><i class="fas fa-list"></i> 카드로</a>
                    </c:otherwise>
                </c:choose>
            </jsp:attribute>
        </t:pageHeader>

        <!-- 오류 메시지 -->
        <c:if test="${not empty error}">
            <div class="ui-alert ui-alert--danger"
                 role="alert"
                 aria-atomic="true">
                <i class="fas fa-exclamation-circle"></i> <c:out value="${error}" />
            </div>
        </c:if>

        <!-- 등록 폼 -->
        <div class="ui-form-card">
            <form id="maintenanceForm"
                  class="ui-form ui-form-layout ui-form-layout--actions-end"
                  method="post"
                  action="${pageContext.request.contextPath}/maintenance"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="add">

                <%@ include file="/WEB-INF/includes/maintenance_form_fields.jspf" %>

                <!-- 버튼 -->
                <div class="button-group ui-form-actions">
                    <c:choose>
                        <c:when test="${not empty customerName}">
                            <a href="<c:out value='${customerHistoryUrl}' />"
                               class="ui-button button--secondary button--md">취소</a>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/maintenance?view=cards"
                               class="ui-button button--secondary button--md">취소</a>
                        </c:otherwise>
                    </c:choose>
                    <button type="submit"
                            class="ui-button button--primary button--md"
                            data-busy-label="등록 중">등록하기</button>
                </div>
            </form>
        </div>
    </div>
</div>



<%@ include file="/includes/footer.jsp" %>
