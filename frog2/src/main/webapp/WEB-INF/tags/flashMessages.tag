<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${not empty requestScope.message}">
    <c:set var="flashTone" value="${requestScope.messageType == 'success' ? 'success' : requestScope.messageType == 'warning' ? 'warning' : requestScope.messageType == 'info' ? 'info' : 'danger'}" />
    <c:set var="flashIcon" value="${flashTone == 'success' ? 'check-circle' : flashTone == 'warning' ? 'exclamation-triangle' : flashTone == 'info' ? 'info-circle' : 'exclamation-circle'}" />
    <div class="ui-alert ui-alert--${flashTone}"
         role="${flashTone == 'danger' ? 'alert' : 'status'}"
         aria-live="${flashTone == 'danger' ? 'assertive' : 'polite'}"
         aria-atomic="true">
        <i class="fas fa-${flashIcon}" aria-hidden="true"></i>
        <span><c:out value="${requestScope.message}" /></span>
    </div>
</c:if>

<c:if test="${not empty sessionScope.message}">
    <div class="ui-alert ui-alert--success"
         role="status"
         aria-live="polite"
         aria-atomic="true">
        <i class="fas fa-check-circle" aria-hidden="true"></i>
        <span><c:out value="${sessionScope.message}" /></span>
    </div>
    <c:remove var="message" scope="session" />
</c:if>

<c:if test="${not empty sessionScope.error}">
    <div class="ui-alert ui-alert--danger"
         role="alert"
         aria-live="assertive"
         aria-atomic="true">
        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
        <span><c:out value="${sessionScope.error}" /></span>
    </div>
    <c:remove var="error" scope="session" />
</c:if>
