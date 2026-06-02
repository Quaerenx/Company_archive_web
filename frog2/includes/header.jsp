<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle} - 게시판 시스템</title>
    <!-- Favicon -->
    <link rel="icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png" sizes="32x32">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png">
    <!-- 기본 스타일시트 임포트 -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main_style.css">
    <!-- 컴포넌트 스타일시트 -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/components.css">
    <!-- 유틸리티 스타일시트 -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/utilities.css">
    <!-- 아카이브 스타일시트 -->
	<%--     <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/archive_style.css"> --%>
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
</head>
<body class="${pageBodyClass}">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>
