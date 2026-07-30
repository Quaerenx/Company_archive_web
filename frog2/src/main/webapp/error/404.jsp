<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <title>페이지를 찾을 수 없습니다 (404)</title>
  <!-- Favicon -->
  <link rel="icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png" sizes="32x32">
  <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png">
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png">
  <link rel="stylesheet" href="<c:url value='/resources/css/pages/error.css' />?v=${initParam.frog2AssetVersion}">
</head>
<body class="error-page error-page--compact">
  <div class="error-card">
    <h1>요청하신 페이지를 찾을 수 없습니다.</h1>
    <p>입력하신 주소가 정확한지 확인해 주세요.</p>
    <p><a href="<c:url value='/dashboard' />">대시보드로 이동</a></p>
  </div>
</body>
</html>
