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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/view-transitions.css?v=${initParam.frog2AssetVersion}">
    <script src="${pageContext.request.contextPath}/resources/js/view-transition-routing.js?v=${initParam.frog2AssetVersion}"></script>
    <%-- 대시보드는 로그인이 받지 않는 스타일시트 7개를 처음 요청한다. 미리 받아두지
         않으면 전환 중 새 문서의 첫 페인트가 늦어 이전 화면이 멈춘 것처럼 보인다. --%>
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/base.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/components.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/ui-table.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/utilities.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/pages/header.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/css/pages/dashboard.css?v=${initParam.frog2AssetVersion}">
    <link rel="prefetch" as="style" href="${pageContext.request.contextPath}/resources/vendor/fontawesome-free/5.15.4/css/all.min.css?v=${initParam.frog2AssetVersion}">
</head>
<body class="ui-system login-page has-ambient-background">
    <canvas class="login-background app-ambient-background"
            data-app-ambient-background
            aria-hidden="true"></canvas>
    <main id="main-content" class="login-shell" tabindex="-1">
        <!-- 카드 뒤에 겹쳐 있다가 hover 시 펼쳐지는 장식용 문서. 실제 데이터는 넣지 않는다. -->
        <div class="login-peek" aria-hidden="true">
            <div class="peek-doc"><div class="peek-sheet"></div></div><!-- 좌상단 -->
            <div class="peek-doc"><div class="peek-sheet"></div></div><!-- 상단 -->
            <div class="peek-doc"><div class="peek-sheet"></div></div><!-- 우상단 -->
        </div>

        <section class="login-card" aria-label="로그인">
            <header class="login-header">
                <div class="login-brand">
                    <%-- 로고 상단 모서리를 난간 삼아 뒤에서 내다보는 장식용 눈.
                         peek 문서가 다 펼쳐진 뒤에 올라온다. 의미 없는 장식이라
                         보조기기에서는 감춘다. --%>
                    <span class="brand-eyes" aria-hidden="true">
                        <span class="brand-eye"><span class="brand-pupil"></span></span>
                        <span class="brand-eye"><span class="brand-pupil"></span></span>
                    </span>
                    <img class="login-brand-logo"
                         src="${pageContext.request.contextPath}/resources/images/archive-logo.svg?v=${initParam.frog2AssetVersion}"
                         width="3664"
                         height="1480"
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
                        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
                        <span><c:out value="${errorMessage}"/></span>
                    </div>
                </c:if>

                <div class="form-group">
                    <input type="text"
                           id="userId"
                           name="userId"
                           placeholder=" "
                           required
                           autocomplete="username"
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

                <label class="login-remember" for="rememberId">
                    <input type="checkbox" id="rememberId">
                    <span>아이디 저장</span>
                </label>

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
    <script src="${pageContext.request.contextPath}/resources/js/pages/login.js?v=${initParam.frog2AssetVersion}"></script>
</body>
</html>
