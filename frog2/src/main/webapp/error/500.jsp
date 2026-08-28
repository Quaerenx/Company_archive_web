<%@ page isErrorPage="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>서버 오류 (500) | Archive</title>
  <!-- Favicon -->
  <%@ include file="/WEB-INF/includes/favicon.jspf" %>
  <link rel="stylesheet" href="<c:url value='/resources/css/tokens.css' />?v=${initParam.frog2AssetVersion}">
  <link rel="stylesheet" href="<c:url value='/resources/css/base.css' />?v=${initParam.frog2AssetVersion}">
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
