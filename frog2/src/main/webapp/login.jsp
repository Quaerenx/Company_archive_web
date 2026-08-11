<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="productName" value="Archive" />
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Archive 로그인">
    <title>로그인 | <c:out value="${productName}" /></title>
    <%@ include file="/WEB-INF/includes/favicon.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/tokens.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-system.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ambient-background.css?v=${initParam.frog2AssetVersion}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/login_style.css?v=${initParam.frog2AssetVersion}">
</head>
<body class="ui-system login-page has-ambient-background">
    <canvas class="login-background app-ambient-background"
            data-app-ambient-background
            aria-hidden="true"></canvas>
    <main id="main-content" class="login-shell" tabindex="-1">
        <section class="login-card" aria-label="로그인">
            <header class="login-header">
                <div class="login-brand">
                    <img class="login-brand-logo"
                         src="${pageContext.request.contextPath}/resources/images/archive-primary-logo.svg"
                         width="1119"
                         height="288"
                         alt="${productName}">
                </div>
            </header>

            <form action="login"
                  method="post"
                  id="loginForm"
                  class="login-form ui-form"
                  autocomplete="off"
                  data-ui-submit-lock="auto">
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>

                <c:if test="${not empty errorMessage}">
                    <div id="login-error"
                         class="login-error ui-alert ui-alert--danger"
                         role="alert"
                         aria-live="assertive"
                         aria-atomic="true">
                        <c:out value="${errorMessage}"/>
                    </div>
                </c:if>

                <div class="form-group">
                    <input type="text"
                           id="userId"
                           name="userId"
                           placeholder=" "
                           required
                           autocomplete="off"
                           autocapitalize="off"
                           autocorrect="off"
                           spellcheck="false"
                           aria-invalid="${not empty errorMessage}"
                           aria-describedby="${not empty errorMessage ? 'login-error' : ''}">
                    <label class="login-field-label" for="userId">아이디</label>
                </div>

                <div class="form-group">
                    <input type="password"
                           id="password"
                           name="password"
                           placeholder=" "
                           required
                           autocomplete="current-password"
                           autocapitalize="off"
                           autocorrect="off"
                           spellcheck="false"
                           aria-invalid="${not empty errorMessage}"
                           aria-describedby="${not empty errorMessage ? 'login-error' : ''}">
                    <label class="login-field-label" for="password">비밀번호</label>
                </div>

                <button type="submit"
                        class="login-submit ui-button button--primary button--md"
                        data-busy-label="로그인">
                    로그인
                </button>
            </form>
        </section>
    </main>
    <script src="${pageContext.request.contextPath}/resources/js/ui-system.js?v=${initParam.frog2AssetVersion}"></script>
    <script src="${pageContext.request.contextPath}/resources/js/ambient-background.js?v=${initParam.frog2AssetVersion}"></script>
</body>
</html>
