<%@ tag language="java" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="idPrefix" required="true" type="java.lang.String" %>
<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="value" required="false" type="java.lang.Object" %>
<%@ attribute name="inputType" required="false" type="java.lang.String" %>
<%@ attribute name="options" required="false" type="java.lang.String" %>
<%@ attribute name="placeholder" required="false" type="java.lang.String" %>
<%@ attribute name="fullWidth" required="false" type="java.lang.Boolean" %>
<%@ attribute name="columnStart" required="false" type="java.lang.Boolean" %>
<%@ attribute name="readonly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="min" required="false" type="java.lang.String" %>

<c:set var="fieldId" value="${idPrefix}-${name}" />
<c:set var="resolvedInputType" value="${empty inputType ? 'text' : inputType}" />
<div class="detail-item${fullWidth ? ' full-width' : ''}${columnStart ? ' detail-item--column-start' : ''}"
     data-customer-detail-field>
    <label class="detail-label" for="<c:out value='${fieldId}' />">
        <c:out value="${label}" />
    </label>
    <div class="detail-value">
        <c:choose>
            <c:when test="${resolvedInputType eq 'select'}">
                <select class="form-control customer-detail-edit-control"
                        id="<c:out value='${fieldId}' />"
                        name="<c:out value='${name}' />">
                    <option value="">선택하세요</option>
                    <c:forTokens items="${options}" delims="|" var="option">
                        <c:set var="separatorIndex" value="${fn:indexOf(option, ':')}" />
                        <c:set var="optionValue" value="${separatorIndex >= 0 ? fn:substring(option, 0, separatorIndex) : option}" />
                        <c:set var="optionLabel" value="${separatorIndex >= 0 ? fn:substring(option, separatorIndex + 1, fn:length(option)) : option}" />
                        <c:choose>
                            <c:when test="${value eq optionValue}">
                                <option value="<c:out value='${optionValue}' />" selected><c:out value="${optionLabel}" /></option>
                            </c:when>
                            <c:otherwise>
                                <option value="<c:out value='${optionValue}' />"><c:out value="${optionLabel}" /></option>
                            </c:otherwise>
                        </c:choose>
                    </c:forTokens>
                </select>
            </c:when>
            <c:when test="${resolvedInputType eq 'textarea'}">
                <textarea class="form-control customer-detail-edit-control note-textarea"
                          id="<c:out value='${fieldId}' />"
                          name="<c:out value='${name}' />"
                          placeholder="<c:out value='${placeholder}' />"><c:out value="${value}" /></textarea>
            </c:when>
            <c:otherwise>
                <input type="<c:out value='${resolvedInputType}' />"
                       class="form-control customer-detail-edit-control${readonly ? ' readonly' : ''}"
                       id="<c:out value='${fieldId}' />"
                       name="<c:out value='${name}' />"
                       value="<c:out value='${value}' />"
                       placeholder="<c:out value='${placeholder}' />"
                       <c:if test="${readonly}">readonly aria-readonly="true"</c:if>
                       <c:if test="${not empty min}">min="<c:out value='${min}' />"</c:if>>
            </c:otherwise>
        </c:choose>
    </div>
</div>
