<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>읽기 전용 환경 | Archive</title>
    <%@ include file="/WEB-INF/includes/favicon.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/tokens.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/base.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/error.css?v=${initParam.frog2AssetVersion}">
</head>
<body class="error-status-page">
    <main class="container error-status">
        <h1>현재 환경은 읽기 전용입니다.</h1>
        <p>데이터를 변경하는 요청은 실행되지 않았습니다.</p>
        <a href="${pageContext.request.contextPath}/dashboard">대시보드로 돌아가기</a>
    </main>
</body>
</html>
