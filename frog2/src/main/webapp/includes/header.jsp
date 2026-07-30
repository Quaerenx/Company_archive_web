<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <c:set var="frog2AssetVersion" value="${initParam.frog2AssetVersion}" scope="request" />
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${not empty pageDocumentTitle ? pageDocumentTitle : pageTitle}" /><c:if test="${empty pageDocumentTitle}"> - 게시판 시스템</c:if></title>
    <!-- Favicon -->
    <link rel="icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png" sizes="32x32">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png">
    <!-- 공통 스타일시트 -->
    <%@ include file="/WEB-INF/includes/core_styles.jspf" %>
    <!-- 아카이브 스타일시트 -->
	<%--     <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/archive_style.css"> --%>
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/header.css?v=${frog2AssetVersion}">
    <c:if test="${not empty pageCss}">
        <c:forTokens items="${pageCss}" delims="," var="stylesheet">
            <link rel="stylesheet" href="${pageContext.request.contextPath}${stylesheet}?v=${frog2AssetVersion}">
        </c:forTokens>
    </c:if>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-system.css?v=${frog2AssetVersion}">
</head>
<body class="ui-system <c:out value='${pageBodyClass}' />">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>
