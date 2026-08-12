<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="hostManagementMode" value="${myPageSection == 'hosts'}" />
<c:set var="pageTitle" value="${hostManagementMode ? '개인 호스트' : '마이페이지'}" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-mypage" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/mypage.css" scope="request" />
<c:if test="${hostManagementMode}">
    <c:set var="pageCss" value="/resources/css/pages/mypage.css,/resources/css/pages/mypage_hosts.css" scope="request" />
    <c:set var="pageScript" value="/resources/js/pages/mypage_hosts.js" scope="request" />
</c:if>

<%@ include file="/includes/header.jsp" %>

<div class="mypage-container content-shell">
    <t:pageHeader>
        <jsp:attribute name="title"><c:out value="${pageTitle}" /></jsp:attribute>
        <jsp:attribute name="subtitle">
            <c:choose>
                <c:when test="${hostManagementMode}">개인 개발·검증용 호스트를 관리합니다.</c:when>
                <c:otherwise>내 정보와 최근 업무를 한눈에 확인합니다.</c:otherwise>
            </c:choose>
        </jsp:attribute>
    </t:pageHeader>

    <c:if test="${not empty message}">
        <c:set var="safeMessageTone" value="${messageType == 'success' ? 'success' : 'danger'}" />
        <div class="ui-alert ui-alert--<c:out value='${safeMessageTone}' />"
             role="${messageType == 'success' ? 'status' : 'alert'}"
             aria-live="${messageType == 'success' ? 'polite' : 'assertive'}"
             aria-atomic="true">
            <c:out value="${message}" />
        </div>
    </c:if>

    <c:choose>
        <c:when test="${hostManagementMode}">
            <%@ include file="/WEB-INF/includes/mypage/host_manager.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="mypage-overview">
                <%@ include file="/WEB-INF/includes/mypage/profile_summary.jspf" %>
                <%@ include file="/WEB-INF/includes/mypage/recent_activity.jspf" %>
                <%@ include file="/WEB-INF/includes/mypage/host_summary.jspf" %>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/includes/footer.jsp" %>
