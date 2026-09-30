<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="customer" required="true" rtexprvalue="true" type="com.company.model.CustomerDTO" %>
<%@ attribute name="registered" required="true" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="frequencyLabel" required="false" rtexprvalue="true" %>

<c:url var="historyUrl" value="/maintenance">
    <c:param name="view" value="history" />
    <c:param name="customerName" value="${customer.customerName}" />
</c:url>
<a class="customer-card"
   href="<c:out value='${historyUrl}' />"
   data-current-month-registered="${registered ? 'true' : 'false'}"
   data-detail-url="<c:out value='${historyUrl}' />">

    <div class="customer-name">
        <i class="fas fa-building"></i>
        <span class="customer-name-text"><c:out value="${customer.customerName}" /></span>
        <c:if test="${frequencyLabel eq '분기'}">
            <span class="maintenance-frequency">분기</span>
        </c:if>
    </div>

    <c:if test="${registered}">
        <i class="fas fa-check-circle maintenance-registration-check"
           role="img"
           aria-label="이번 달 등록 완료"></i>
    </c:if>

    <div class="customer-info">
        <div class="info-row">
            <span class="info-label">DB명</span>
            <span class="info-value" title="<c:out value='${customer.dbName}' />"><c:out value="${customer.dbName}" /></span>
        </div>
        <div class="info-row">
            <span class="info-label">버전</span>
            <span class="info-value">
                <c:if test="${not empty customer.verticaVersion}">
                    <span class="version-badge ui-badge ui-badge--neutral"><c:out value="${customer.verticaVersion}" /></span>
                </c:if>
            </span>
        </div>
        <div class="info-row">
            <span class="info-label">모드</span>
            <span class="info-value">
                <c:if test="${not empty customer.mode}">
                    <span class="mode-badge ui-badge ui-badge--neutral"><c:out value="${customer.mode}" /></span>
                </c:if>
            </span>
        </div>
        <div class="info-row">
            <span class="info-label">노드수</span>
            <span class="info-value"><c:out value="${customer.nodes}" /></span>
        </div>
    </div>

</a>
