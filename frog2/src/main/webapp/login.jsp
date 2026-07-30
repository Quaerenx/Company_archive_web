<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="productName" value="ARCHIVE" />
<c:set var="loginDescriptionIds" value="login-help" />
<c:if test="${not empty errorMessage}">
    <c:set var="loginDescriptionIds" value="login-help login-error" />
</c:if>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="ARCHIVE 사내 운영 기록 서비스 로그인">
    <title>로그인 | <c:out value="${productName}" /></title>
    <link rel="icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png" sizes="32x32">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/favicon.png">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.png" type="image/png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/tokens.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/login_style.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-system.css?v=${initParam.frog2AssetVersion}">
</head>
<body class="ui-system login-page">
    <main class="login-shell">
        <section class="login-card" aria-labelledby="login-title">
            <header class="login-header">
                <div class="login-brand" aria-label="${productName}">
                    <span class="login-brand-mark" aria-hidden="true">A</span>
                    <span class="login-brand-name"><c:out value="${productName}" /></span>
                </div>
                <p class="login-eyebrow">사내 운영 기록 서비스</p>
                <h1 id="login-title">업무 계정으로 로그인</h1>
                <p class="login-intro">
                    정기점검, 고객 지원 기록과 운영 자료를 한곳에서 확인하세요.
                </p>
            </header>

            <form action="login"
                  method="post"
                  id="loginForm"
                  class="login-form ui-form"
                  autocomplete="on">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>

                <c:if test="${not empty errorMessage}">
                    <div id="login-error"
                         class="login-error ui-alert ui-alert--danger"
                         role="alert"
                         aria-live="assertive"
                         aria-atomic="true">
                        <span class="login-error-icon" aria-hidden="true">!</span>
                        <c:out value="${errorMessage}"/>
                    </div>
                </c:if>

                <div class="form-group">
                    <label for="userId">아이디</label>
                    <input type="text"
                           id="userId"
                           name="userId"
                           required
                           autocomplete="username"
                           autocapitalize="off"
                           autocorrect="off"
                           spellcheck="false"
                           aria-describedby="${loginDescriptionIds}">
                </div>

                <div class="form-group">
                    <label for="password">비밀번호</label>
                    <input type="password"
                           id="password"
                           name="password"
                           required
                           autocomplete="current-password"
                           autocapitalize="off"
                           autocorrect="off"
                           spellcheck="false"
                           aria-describedby="${loginDescriptionIds}">
                </div>

                <p id="login-help" class="login-help ui-help-text">
                    승인된 사내 계정을 사용해 주세요.
                </p>

                <div class="remember-id">
                    <input type="checkbox" id="rememberId" name="rememberId">
                    <label for="rememberId">아이디 저장</label>
                </div>

                <button type="submit"
                        class="login-submit ui-button button--primary button--md">
                    로그인
                </button>
            </form>

            <footer class="login-footer">
                <p><c:out value="${productName}" /> · 사내 업무용 서비스</p>
            </footer>
        </section>
    </main>
    <script src="${pageContext.request.contextPath}/resources/js/pages/login.js?v=${initParam.frog2AssetVersion}"></script>
</body>
</html>
