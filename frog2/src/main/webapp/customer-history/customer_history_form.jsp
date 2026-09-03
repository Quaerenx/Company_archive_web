<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="isHistoryEdit" value="${formMode eq 'edit'}" />
<c:set var="pageTitle" value="${isHistoryEdit ? '고객사 히스토리 수정' : '고객사 히스토리 등록'}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customer-history" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/customer_history.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/customer_history.js" scope="request" />
<%@ include file="/includes/header.jsp" %>

<div class="customer-history customer-history-form-page content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-history" aria-hidden="true"></i>
            ${isHistoryEdit ? '고객사 히스토리 수정' : '새 고객사 히스토리 등록'}
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            정기점검이 아닌 주요 장애, 업그레이드, 증설 작업만 기록합니다.
        </jsp:attribute>
        <jsp:attribute name="actions">
            <a href="<c:out value='${returnListUrl}' />"
               class="ui-button button--secondary button--sm">
                <i class="fas fa-arrow-left" aria-hidden="true"></i>
                목록으로
            </a>
        </jsp:attribute>
    </t:pageHeader>

    <c:if test="${not empty formError}">
        <div class="ui-alert ui-alert--danger"
             role="alert"
             aria-live="assertive">
            <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
            <span><c:out value="${formError}" /></span>
        </div>
    </c:if>

    <section class="customer-history-form-card ui-form-card"
             aria-labelledby="customer-history-form-title">
        <h2 id="customer-history-form-title" class="ui-section-title">작업 정보</h2>
        <form class="ui-form customer-history-form"
              method="post"
              action="${pageContext.request.contextPath}/customer-history"
              data-ui-draft="auto"
              data-ui-draft-id="customer-history:${isHistoryEdit ? formId : 'new'}"
              data-ui-draft-success-views="list"
              data-ui-dirty-guard="auto"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="action" value="${isHistoryEdit ? 'update' : 'add'}">
            <input type="hidden" name="returnCustomerName" value="<c:out value='${formReturnCustomerName}' />">
            <input type="hidden" name="returnCategory" value="<c:out value='${formReturnCategory}' />">
            <input type="hidden" name="returnQ" value="<c:out value='${formReturnQ}' />">
            <input type="hidden" name="returnPage" value="<c:out value='${formReturnPage}' />">
            <c:if test="${isHistoryEdit}">
                <input type="hidden" name="id" value="<c:out value='${formId}' />">
            </c:if>

            <div class="customer-history-form-grid">
                <label class="form-group">
                    <span>고객사 <span aria-hidden="true">*</span></span>
                    <select name="customerName" required>
                        <option value="">고객사를 선택하세요</option>
                        <c:forEach var="customer" items="${customerList}">
                            <option value="<c:out value='${customer.customerName}' />"
                                    ${formCustomerName eq customer.customerName ? 'selected' : ''}>
                                <c:out value="${customer.customerName}" />
                            </option>
                        </c:forEach>
                    </select>
                </label>
                <label class="form-group">
                    <span>작업일 <span aria-hidden="true">*</span></span>
                    <input type="date"
                           name="workDate"
                           value="<c:out value='${formWorkDate}' />"
                           required>
                </label>
                <label class="form-group">
                    <span>작업 유형 <span aria-hidden="true">*</span></span>
                    <select name="category" required>
                        <option value="">유형을 선택하세요</option>
                        <c:forEach var="historyCategory" items="${historyCategories}">
                            <option value="${historyCategory.code}"
                                    ${formCategory eq historyCategory.code ? 'selected' : ''}>
                                <c:out value="${historyCategory.label}" />
                            </option>
                        </c:forEach>
                    </select>
                </label>
                <input type="hidden" name="status" value="completed">
            </div>

            <label class="form-group customer-history-title-field">
                <span>이력 <span aria-hidden="true">*</span></span>
                <input type="text"
                       name="title"
                       value="<c:out value='${formTitle}' />"
                       maxlength="300"
                       required
                       placeholder="예: 개발서버 3번 노드 다운 현장지원">
            </label>

            <label class="form-group customer-history-action-field">
                <span>조치사항 <span aria-hidden="true">*</span></span>
                <textarea name="actionSummary"
                          maxlength="4000"
                          rows="7"
                          required
                          placeholder="원인, 수행한 조치와 결과를 간결하게 입력하세요."><c:out value="${formActionSummary}" /></textarea>
            </label>

            <div class="ui-form-actions">
                <a href="<c:out value='${returnListUrl}' />"
                   class="ui-button button--secondary button--md">취소</a>
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="저장 중">
                    ${isHistoryEdit ? '수정하기' : '등록하기'}
                </button>
            </div>
        </form>

        <c:if test="${isHistoryEdit}">
            <form class="customer-history-delete-form"
                  method="post"
                  action="${pageContext.request.contextPath}/customer-history"
                  data-customer-history-delete>
                <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="id" value="<c:out value='${formId}' />">
                <input type="hidden" name="returnCustomerName" value="<c:out value='${formReturnCustomerName}' />">
                <input type="hidden" name="returnCategory" value="<c:out value='${formReturnCategory}' />">
                <input type="hidden" name="returnQ" value="<c:out value='${formReturnQ}' />">
                <input type="hidden" name="returnPage" value="<c:out value='${formReturnPage}' />">
                <button type="submit"
                        class="ui-button button--danger button--sm">이력 삭제</button>
            </form>
        </c:if>
    </section>
</div>

<%@ include file="/includes/footer.jsp" %>
