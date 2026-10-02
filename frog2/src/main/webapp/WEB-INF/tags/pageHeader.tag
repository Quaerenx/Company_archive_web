<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="title" fragment="true" required="true" %>
<%@ attribute name="subtitle" fragment="true" required="false" %>
<%@ attribute name="actions" fragment="true" required="false" %>
<%@ attribute name="extra" fragment="true" required="false" %>

<div class="page-header">
  <c:if test="${not empty requestScope.pageBreadcrumbSection}">
    <nav class="app-breadcrumb" aria-label="현재 위치">
      <ol>
        <c:choose>
          <c:when test="${requestScope.pageBreadcrumbSection eq 'dashboard' and requestScope.pageTitle eq '대시보드'}">
            <li><span aria-current="page"><c:out value="${requestScope.pageTitle}" /></span></li>
          </c:when>
          <c:otherwise>
            <li><a href="${pageContext.request.contextPath}/dashboard">대시보드</a></li>
            <c:choose>
              <c:when test="${requestScope.pageBreadcrumbSection eq 'customers'}">
                <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span>고객관리</span></li>
              </c:when>
              <c:when test="${requestScope.pageBreadcrumbSection eq 'resources'}">
                <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span>자료관리</span></li>
              </c:when>
              <c:when test="${requestScope.pageBreadcrumbSection eq 'mypage' and requestScope.pageTitle ne '마이페이지'}">
                <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><a href="${pageContext.request.contextPath}/mypage">마이페이지</a></li>
              </c:when>
            </c:choose>
            <li><span class="app-breadcrumb__separator" aria-hidden="true">›</span><span aria-current="page"><c:out value="${requestScope.pageTitle}" /></span></li>
          </c:otherwise>
        </c:choose>
      </ol>
    </nav>
  </c:if>

  <div class="ph-header">
    <div class="ph-left">
      <div class="ph-title"><h1><jsp:invoke fragment="title"/></h1></div>
      <c:if test="${not empty subtitle}">
        <div class="ph-subtitle"><jsp:invoke fragment="subtitle"/></div>
      </c:if>
    </div>

    <c:if test="${not empty actions}">
      <div class="ph-actions"><jsp:invoke fragment="actions"/></div>
    </c:if>
  </div>

  <c:if test="${not empty extra}">
    <div class="ph-extra">
      <jsp:invoke fragment="extra"/>
    </div>
  </c:if>
</div>
