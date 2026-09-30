<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="customers" required="true" rtexprvalue="true" type="java.util.List" %>
<%@ attribute name="assignedCustomers" required="true" rtexprvalue="true" type="java.util.Map" %>
<%@ attribute name="currentUserName" required="false" rtexprvalue="true" %>
<%@ attribute name="returnFilter" required="false" rtexprvalue="true" %>
<%@ attribute name="returnSortField" required="false" rtexprvalue="true" %>
<%@ attribute name="returnSortDirection" required="false" rtexprvalue="true" %>
<%@ attribute name="returnQuery" required="false" rtexprvalue="true" %>
<%@ attribute name="returnPage" required="false" rtexprvalue="true" %>
<%@ attribute name="returnPageSize" required="false" rtexprvalue="true" %>

<c:forEach var="customer" items="${customers}">
    <c:url var="customerDetailUrl" value="/customers">
        <c:param name="view" value="detail" />
        <c:param name="customerName" value="${customer.customerName}" />
        <c:param name="returnFilter" value="${returnFilter}" />
        <c:param name="returnSortField" value="${returnSortField}" />
        <c:param name="returnSortDirection" value="${returnSortDirection}" />
        <c:param name="returnQ" value="${returnQuery}" />
        <c:param name="returnPage" value="${returnPage}" />
        <c:param name="returnPageSize" value="${returnPageSize}" />
    </c:url>
    <tr class="customer-row ui-data-row"
        data-ui-return-row
        data-ui-return-key="<c:out value='${customer.customerName}' />"
        data-assigned-customer="${assignedCustomers[customer.customerName] ? 'true' : 'false'}"
        data-detail-url="<c:out value="${customerDetailUrl}" />">
        <td class="col--customer" title="<c:out value="${customer.customerName}" />" data-original="<c:out value="${customer.customerName}" />">
            <a class="customer-detail-link"
               href="<c:out value='${customerDetailUrl}' />"><c:out value="${customer.customerName}" default="" /></a>
            <dl class="customer-mobile-meta">
                <div>
                    <dt>버전</dt>
                    <dd><c:out value="${customer.verticaVersion}" default="-" /></dd>
                </div>
                <div>
                    <dt>모드</dt>
                    <dd><c:out value="${customer.mode}" default="-" /></dd>
                </div>
                <div>
                    <dt>OS</dt>
                    <dd><c:out value="${customer.os}" default="-" /></dd>
                </div>
                <div>
                    <dt>노드</dt>
                    <dd><c:out value="${customer.nodes}" default="-" /></dd>
                </div>
                <div>
                    <dt>라이선스</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${not empty customer.licenseAmount}">
                                <c:out value="${customer.licenseAmount}" /><c:if test="${not empty customer.licenseUnit}"> <c:out value="${customer.licenseUnit}" /></c:if>
                            </c:when>
                            <c:otherwise><c:out value="${customer.licenseSize}" default="-" /></c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <div>
                    <dt>담당자</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${assignedCustomers[customer.customerName]}">
                                <span class="customer-assignee-badge ui-badge ui-badge--info"><c:out value="${currentUserName}" /></span>
                            </c:when>
                            <c:otherwise><c:out value="${customer.managerName}" default="-" /></c:otherwise>
                        </c:choose>
                    </dd>
                </div>
                <div class="customer-mobile-meta__wide">
                    <dt>SAID</dt>
                    <dd><c:out value="${customer.said}" default="-" /></dd>
                </div>
            </dl>
        </td>
        <td class="customer-col-version col--identifier" data-original="<c:out value="${customer.verticaVersion}" />"><c:out value="${customer.verticaVersion}" default="-" /></td>
        <td class="customer-col-mode col--type" data-original="<c:out value="${customer.mode}" />"><c:out value="${customer.mode}" default="-" /></td>
        <td class="col--text" data-original="<c:out value="${customer.os}" />"><c:out value="${customer.os}" default="-" /></td>
        <td class="customer-col-nodes col--numeric" data-original="<c:out value="${customer.nodes}" />"><c:out value="${customer.nodes}" default="-" /></td>
        <td class="customer-col-license col--numeric" data-original="<c:out value="${customer.licenseSize}" />">
            <c:choose>
                <c:when test="${not empty customer.licenseAmount}">
                    <span class="customer-license-amount"><c:out value="${customer.licenseAmount}" /></span><c:if test="${not empty customer.licenseUnit}"> <span class="customer-license-unit"><c:out value="${customer.licenseUnit}" /></span></c:if>
                </c:when>
                <c:otherwise><c:out value="${customer.licenseSize}" default="-" /></c:otherwise>
            </c:choose>
        </td>
        <td class="col--identifier" data-original="<c:out value="${customer.said}" />"><c:out value="${customer.said}" default="-" /></td>
        <td class="col--author" title="<c:out value='${assignedCustomers[customer.customerName] ? currentUserName : customer.managerName}' />" data-original="<c:out value="${customer.managerName}" />">
            <c:choose>
                <c:when test="${assignedCustomers[customer.customerName]}">
                    <span class="customer-assignee-badge ui-badge ui-badge--info"><c:out value="${currentUserName}" /></span>
                </c:when>
                <c:otherwise><c:out value="${customer.managerName}" default="-" /></c:otherwise>
            </c:choose>
        </td>
    </tr>
</c:forEach>
