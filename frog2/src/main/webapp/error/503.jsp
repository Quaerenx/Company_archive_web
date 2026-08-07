<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>서비스 일시 중단 | Archive</title>
    <%@ include file="/WEB-INF/includes/favicon.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/tokens.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/base.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/error.css?v=${initParam.frog2AssetVersion}">
</head>
<body class="error-status-page">
    <main class="container error-status">
        <h1>데이터 서비스를 일시적으로 사용할 수 없습니다.</h1>
        <p>잠시 후 다시 시도해 주세요.</p>
        <a href="${pageContext.request.contextPath}/dashboard">대시보드로 돌아가기</a>
    </main>
</body>
</html>
