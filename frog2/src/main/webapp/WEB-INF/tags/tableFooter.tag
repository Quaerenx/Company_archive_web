<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="totalCount" required="true" rtexprvalue="true" %>
<%@ attribute name="itemLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="currentPage" required="true" rtexprvalue="true" %>
<%@ attribute name="totalPages" required="true" rtexprvalue="true" %>
<%@ attribute name="previousUrl" required="false" rtexprvalue="true" %>
<%@ attribute name="nextUrl" required="false" rtexprvalue="true" %>
<%@ attribute name="paginationLabel" required="true" rtexprvalue="true" %>

<c:set var="tableFooterCurrentPage" value="${currentPage ge 1 ? currentPage : 1}" />
<c:set var="tableFooterTotalPages" value="${totalPages ge 1 ? totalPages : 1}" />
<c:set var="previousLabel" value="${itemLabel} 이전 페이지" />
<c:set var="nextLabel" value="${itemLabel} 다음 페이지" />

<footer class="ui-table-footer">
    <p class="ui-table-footer__count">
        총 <strong><c:out value="${totalCount}" /></strong>개 <c:out value="${itemLabel}" />
    </p>
    <nav class="ui-table-pagination" aria-label="${paginationLabel}">
        <c:choose>
            <c:when test="${not empty previousUrl}">
                <a class="ui-table-pagination__control"
                   href="<c:out value='${previousUrl}' />"
                   aria-label="${previousLabel}">&lsaquo;</a>
            </c:when>
            <c:otherwise>
                <span class="ui-table-pagination__control is-disabled"
                      aria-hidden="true">&lsaquo;</span>
            </c:otherwise>
        </c:choose>
        <span class="ui-table-pagination__position">
            <span class="sr-only">현재 페이지</span>
            <c:out value="${tableFooterCurrentPage}" />
            <span aria-hidden="true"> / </span>
            <c:out value="${tableFooterTotalPages}" />
        </span>
        <c:choose>
            <c:when test="${not empty nextUrl}">
                <a class="ui-table-pagination__control"
                   href="<c:out value='${nextUrl}' />"
                   aria-label="${nextLabel}">&rsaquo;</a>
            </c:when>
            <c:otherwise>
                <span class="ui-table-pagination__control is-disabled"
                      aria-hidden="true">&rsaquo;</span>
            </c:otherwise>
        </c:choose>
    </nav>
</footer>
