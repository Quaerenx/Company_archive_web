<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <c:set var="frog2AssetVersion" value="${initParam.frog2AssetVersion}" scope="request" />
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${not empty pageDocumentTitle ? pageDocumentTitle : pageTitle}" /> | Archive</title>
    <!-- Favicon -->
    <%@ include file="/WEB-INF/includes/favicon.jspf" %>
    <!-- 공통 스타일시트 -->
    <%@ include file="/WEB-INF/includes/core_styles.jspf" %>
    <!-- Font Awesome Free 5.15.4 (self-hosted; see resources/vendor/THIRD_PARTY_NOTICES.md) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/vendor/fontawesome-free/5.15.4/css/all.min.css?v=${frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/header.css?v=${frog2AssetVersion}">
    <c:if test="${not empty pageCss}">
        <c:forTokens items="${pageCss}" delims="," var="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}${stylesheet}?v=${frog2AssetVersion}">
        </c:forTokens>
    </c:if>
    <c:if test="${not empty pageHeadScript}">
        <c:forTokens items="${pageHeadScript}" delims="," var="script">
            <script src="${pageContext.request.contextPath}${script}?v=${frog2AssetVersion}"></script>
        </c:forTokens>
    </c:if>
</head>
<body class="ui-system has-ambient-background authenticated-shell <c:out value='${pageBodyClass}' />"
      data-user-id="<c:out value='${sessionScope.user.userId}' />">
<a class="skip-link" href="#main-content">본문으로 건너뛰기</a>
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>
<main id="main-content" class="app-main" tabindex="-1">
<c:if test="${(navDashboardCurrent and pageTitle eq '대시보드') or navCustomerSectionCurrent or navResourceSectionCurrent or navMyPageCurrent}">
    <nav class="app-breadcrumb content-shell" aria-label="현재 위치">
        <ol>
            <c:choose>
                <c:when test="${navDashboardCurrent and pageTitle eq '대시보드'}">
                    <li><span aria-current="page"><c:out value="${pageTitle}" /></span></li>
                </c:when>
                <c:otherwise>
                    <li><a href="${pageContext.request.contextPath}/dashboard">대시보드</a></li>
                    <c:choose>
                        <c:when test="${navCustomerSectionCurrent}">
                            <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span>고객관리</span></li>
                        </c:when>
                        <c:when test="${navResourceSectionCurrent}">
                            <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span>자료관리</span></li>
                        </c:when>
                        <c:when test="${pageTitle ne '마이페이지'}">
                            <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><a href="${pageContext.request.contextPath}/mypage">마이페이지</a></li>
                        </c:when>
                    </c:choose>
                    <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span aria-current="page"><c:out value="${pageTitle}" /></span></li>
                </c:otherwise>
            </c:choose>
        </ol>
    </nav>
</c:if>
