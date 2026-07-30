<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>요청을 처리할 수 없습니다 (403)</title>
  <link rel="icon" href="<c:url value='/favicon.png' />" type="image/png" sizes="32x32">
  <link rel="stylesheet" href="<c:url value='/resources/css/pages/error.css' />?v=${initParam.frog2AssetVersion}">
</head>
<body class="error-page error-page--compact">
  <div class="error-card">
    <h1>요청을 확인할 수 없습니다.</h1>
    <p>페이지를 새로 고친 뒤 다시 시도해 주세요.</p>
    <p><a href="<c:url value='/dashboard' />">대시보드로 이동</a></p>
  </div>
</body>
</html>
