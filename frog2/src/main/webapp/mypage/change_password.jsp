<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="비밀번호 변경" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-mypage" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/password_change.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/password_change.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

<div class="password-container account-form-container content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-key"></i> 비밀번호 변경
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            새로운 비밀번호로 변경
        </jsp:attribute>
    </t:pageHeader>

    <div class="form-card ui-form-card">
        <div class="password-requirements">
            <h3>
                <i class="fas fa-info-circle password-requirement-icon"></i>
                비밀번호 요구사항
            </h3>
            <ul>
                <li>최소 8자 이상</li>
                <li>영문, 숫자, 특수문자 조합 권장</li>
                <li>현재 비밀번호와 다른 비밀번호 사용</li>
            </ul>
        </div>

        <c:if test="${not empty message}">
            <c:set var="safeMessageTone" value="${messageType == 'success' ? 'success' : 'danger'}" />
            <c:set var="safeMessageIcon" value="${messageType == 'success' ? 'check-circle' : 'exclamation-circle'}" />
            <div class="ui-alert ui-alert--<c:out value='${safeMessageTone}' />"
                 role="${messageType == 'success' ? 'status' : 'alert'}"
                 aria-live="${messageType == 'success' ? 'polite' : 'assertive'}"
                 aria-atomic="true">
                <i class="fas fa-<c:out value='${safeMessageIcon}' />"></i>
                <c:out value="${message}" />
            </div>
        </c:if>

        <form id="passwordChangeForm"
              class="ui-form"
              action="${pageContext.request.contextPath}/mypage"
              method="post"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="formAction" value="updatePassword">

            <div class="form-group">
                <label class="form-label" for="currentPassword">
                    <i class="fas fa-lock"></i> 현재 비밀번호 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="currentPassword" name="currentPassword"
                       required autocomplete="current-password" placeholder="현재 비밀번호를 입력하세요">
            </div>

            <div class="form-group">
                <label class="form-label" for="newPassword">
                    <i class="fas fa-lock-open"></i> 새 비밀번호 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="newPassword" name="newPassword"
                       required autocomplete="new-password" placeholder="새 비밀번호를 입력하세요">
                <div class="help-text">최소 8자 이상 입력해주세요.</div>
            </div>

            <div class="form-group">
                <label class="form-label" for="confirmPassword">
                    <i class="fas fa-check-circle"></i> 새 비밀번호 확인 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                       required autocomplete="new-password" placeholder="새 비밀번호를 다시 입력하세요">
            </div>

            <div class="form-actions">
                <button type="submit"
                        class="btn btn-primary ui-button button--primary button--md"
                        data-busy-label="변경 중">
                    <i class="fas fa-save"></i>
                    변경
                </button>
                <a href="${pageContext.request.contextPath}/mypage"
                   class="btn btn-secondary ui-button button--secondary button--md">
                    <i class="fas fa-times"></i>
                    취소
                </a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/includes/footer.jsp" %>
