<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="customer" required="true" rtexprvalue="true" type="com.company.model.CustomerDTO" %>
<%@ attribute name="registered" required="true" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="due" required="true" rtexprvalue="true" type="java.lang.Boolean" %>
<%@ attribute name="monthLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="frequencyLabel" required="false" rtexprvalue="true" %>

<c:choose>
    <c:when test="${registered}">
        <c:set var="registrationState" value="registered" />
        <c:set var="registrationLabel" value="이력 등록" />
    </c:when>
    <c:when test="${due}">
        <c:set var="registrationState" value="unregistered" />
        <c:set var="registrationLabel" value="이력 미등록" />
    </c:when>
    <c:otherwise>
        <c:set var="registrationState" value="not-due" />
        <c:set var="registrationLabel" value="점검 대상 아님" />
    </c:otherwise>
</c:choose>

<c:url var="historyUrl" value="/maintenance">
    <c:param name="view" value="history" />
    <c:param name="customerName" value="${customer.customerName}" />
</c:url>
<a class="customer-card"
   href="<c:out value='${historyUrl}' />"
   data-current-month-registered="${registered ? 'true' : 'false'}"
   data-detail-url="<c:out value='${historyUrl}' />">

    <div class="customer-name">
        <i class="fas fa-building" aria-hidden="true"></i>
        <span class="customer-name-text" title="<c:out value='${customer.customerName}' />"><c:out value="${customer.customerName}" /></span>
        <c:if test="${frequencyLabel eq '분기'}">
            <span class="maintenance-frequency">분기</span>
        </c:if>
        <i class="fas fa-chevron-right customer-card-arrow" aria-hidden="true"></i>
    </div>

    <span class="maintenance-registration-status maintenance-registration-status--${registrationState}"
          data-maintenance-registration-status="${registrationState}">
        <c:choose>
            <c:when test="${registered}"><i class="fas fa-check-circle" aria-hidden="true"></i></c:when>
            <c:when test="${due}"><i class="far fa-circle" aria-hidden="true"></i></c:when>
            <c:otherwise><i class="fas fa-minus-circle" aria-hidden="true"></i></c:otherwise>
        </c:choose>
        <span><c:out value="${monthLabel}" /> <c:out value="${registrationLabel}" /></span>
    </span>

    <dl class="customer-info">
        <div class="info-row">
            <dt class="info-label">DB명</dt>
            <dd class="info-value" title="<c:out value='${customer.dbName}' />"><c:out value="${customer.dbName}" /></dd>
        </div>
        <div class="info-row">
            <dt class="info-label">버전</dt>
            <dd class="info-value" title="<c:out value='${customer.verticaVersion}' />">
                <c:if test="${not empty customer.verticaVersion}">
                    <span class="version-badge ui-badge ui-badge--neutral"><c:out value="${customer.verticaVersion}" /></span>
                </c:if>
            </dd>
        </div>
        <div class="info-row">
            <dt class="info-label">모드</dt>
            <dd class="info-value" title="<c:out value='${customer.mode}' />">
                <c:if test="${not empty customer.mode}">
                    <span class="mode-badge ui-badge ui-badge--neutral"><c:out value="${customer.mode}" /></span>
                </c:if>
            </dd>
        </div>
        <div class="info-row">
            <dt class="info-label">노드수</dt>
            <dd class="info-value"><c:out value="${customer.nodes}" /></dd>
        </div>
    </dl>

</a>
