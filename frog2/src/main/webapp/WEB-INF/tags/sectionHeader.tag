<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="title" fragment="true" required="true" %>
<%@ attribute name="actions" fragment="true" required="false" %>
<%@ attribute name="className" required="false" rtexprvalue="true" %>
<%@ attribute name="flush" required="false" type="java.lang.Boolean" %>
<%@ attribute name="compact" required="false" type="java.lang.Boolean" %>

<header class="ui-section-header${flush ? ' ui-section-header--flush' : ''}${compact ? ' ui-section-header--compact' : ''}${not empty className ? ' ' : ''}${className}">
    <jsp:invoke fragment="title" />
    <c:if test="${not empty actions}">
        <div class="ui-section-actions">
            <jsp:invoke fragment="actions" />
        </div>
    </c:if>
</header>
