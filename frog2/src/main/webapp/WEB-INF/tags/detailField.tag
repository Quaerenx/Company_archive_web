<%@ tag language="java" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="value" required="false" type="java.lang.Object" %>
<%@ attribute name="fullWidth" required="false" type="java.lang.Boolean" %>
<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>

<div class="detail-item${fullWidth ? ' full-width' : ''}">
    <span class="detail-label"><c:out value="${label}" /></span>
    <c:choose>
        <c:when test="${empty value}">
            <span class="detail-value detail-value--empty">미등록</span>
        </c:when>
        <c:when test="${multiline}">
            <div class="detail-value note-content"><c:out value="${value}" /></div>
        </c:when>
        <c:otherwise>
            <span class="detail-value"><c:out value="${value}" /></span>
        </c:otherwise>
    </c:choose>
</div>
