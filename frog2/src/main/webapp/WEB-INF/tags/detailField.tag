<%@ tag language="java" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="value" required="false" type="java.lang.Object" %>
<%@ attribute name="fullWidth" required="false" type="java.lang.Boolean" %>
<%@ attribute name="columnStart" required="false" type="java.lang.Boolean" %>
<%@ attribute name="multiline" required="false" type="java.lang.Boolean" %>
<%@ attribute name="booleanState" required="false" type="java.lang.Boolean" %>
<%@ attribute name="hideWhenEmpty" required="false" type="java.lang.Boolean" %>
<%@ attribute name="suffix" required="false" type="java.lang.String" %>

<div class="detail-item${fullWidth ? ' full-width' : ''}${columnStart ? ' detail-item--column-start' : ''}${hideWhenEmpty && empty value ? ' detail-item--empty' : ''}"
     ${hideWhenEmpty && empty value ? 'hidden' : ''}>
    <span class="detail-label"><c:out value="${label}" /></span>
    <c:choose>
        <c:when test="${empty value}">
            <span class="detail-value detail-value--empty">미등록</span>
        </c:when>
        <c:when test="${multiline}">
            <div class="detail-value note-content"><c:out value="${value}" /></div>
        </c:when>
        <c:when test="${booleanState}">
            <c:set var="normalizedBooleanValue" value="${fn:toUpperCase(fn:trim(value))}" />
            <c:choose>
                <c:when test="${normalizedBooleanValue eq 'Y'}">
                    <span class="detail-value detail-status detail-status--enabled"><span class="detail-status-dot" aria-hidden="true"></span><span>사용</span></span>
                </c:when>
                <c:when test="${normalizedBooleanValue eq 'N'}">
                    <span class="detail-value detail-status detail-status--disabled"><span>미사용</span></span>
                </c:when>
                <c:otherwise>
                    <span class="detail-value"><c:out value="${value}" /></span>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <span class="detail-value"><c:out value="${value}" /><c:if test="${not empty suffix}"> <c:out value="${suffix}" /></c:if></span>
        </c:otherwise>
    </c:choose>
</div>
