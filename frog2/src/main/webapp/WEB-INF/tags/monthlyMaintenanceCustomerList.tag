<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="customers" required="true" rtexprvalue="true" type="java.util.List" %>

<ul class="maintenance-assignee-customers">
  <c:forEach var="customer" items="${customers}">
    <c:url value="/maintenance" var="customerHistoryUrl">
      <c:param name="view" value="history" />
      <c:param name="customerName" value="${customer.customerName}" />
    </c:url>
    <li class="maintenance-assignee-customer maintenance-assignee-customer--${customer.statusCode}"
        data-maintenance-status="${customer.statusCode}">
      <a href="${customerHistoryUrl}">
        <span class="maintenance-assignee-customer-name">
          <c:out value="${customer.customerName}" />
        </span>
        <c:if test="${customer.quarterly}">
          <span class="maintenance-assignee-frequency" aria-label="분기 점검">분기</span>
        </c:if>
        <span class="maintenance-assignee-status">
          <c:out value="${customer.statusLabel}" />
        </span>
      </a>
    </li>
  </c:forEach>
</ul>
