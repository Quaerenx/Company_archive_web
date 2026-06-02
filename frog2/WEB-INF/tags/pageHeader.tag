<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="title" fragment="true" required="true" %>
<%@ attribute name="subtitle" fragment="true" required="false" %>
<%@ attribute name="actions" fragment="true" required="false" %>
<%@ attribute name="extra" fragment="true" required="false" %>

<div class="page-header">
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
