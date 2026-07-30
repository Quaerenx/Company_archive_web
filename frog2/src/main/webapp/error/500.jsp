<%@ page isErrorPage="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <title>서버 오류 (500)</title>
  <!-- Favicon -->
  <link rel="icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png" sizes="32x32">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png">
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png">
  <link rel="stylesheet" href="<c:url value='/resources/css/pages/error.css' />?v=${initParam.frog2AssetVersion}">
</head>
<body class="error-page error-page--server">
  <div class="error-card">
    <h1>처리 중 문제가 발생했습니다.</h1>
    <p>잠시 후 다시 시도해 주세요. 문제가 지속되면 관리자에게 문의하세요.</p>
    <p><a href="<c:url value='/dashboard' />">대시보드로 이동</a></p>
  </div>
</body>
</html>
