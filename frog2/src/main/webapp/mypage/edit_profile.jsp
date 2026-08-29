<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="프로필 수정" scope="request" />
<c:set var="pageDocumentTitle" value="${pageTitle}" scope="request" />
<c:set var="pageBodyClass" value="page-1050 page-customers page-mypage" scope="request" />
<c:set var="pageCss" value="/resources/css/pages/profile_edit.css" scope="request" />
<c:set var="pageScript" value="/resources/js/pages/profile_edit.js" scope="request" />

<%@ include file="/includes/header.jsp" %>

<div class="edit-container account-form-container content-shell">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-user-edit"></i> 프로필 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            사용자 정보 수정
        </jsp:attribute>
    </t:pageHeader>

    <t:flashMessages />

    <div class="ui-form-card">
        <form id="profileEditForm"
              class="ui-form"
              action="${pageContext.request.contextPath}/mypage"
              method="post"
              data-ui-submit-lock="auto">
            <%@ include file="/WEB-INF/includes/csrf_input.jspf" %>
            <input type="hidden" name="formAction" value="updateProfile">

            <div class="form-group">
                <label class="form-label" for="userId">
                    <i class="fas fa-id-card"></i> 아이디
                </label>
                <input type="text" class="form-control" id="userId" name="userId"
                       value="<c:out value="${userInfo.userId}" />" disabled>
                <div class="help-text">아이디는 변경할 수 없습니다.</div>
            </div>

            <div class="form-group">
                <label class="form-label" for="userName">
                    <i class="fas fa-user"></i> 이름 <span class="required">*</span>
                </label>
                <input type="text" class="form-control" id="userName" name="userName"
                       value="<c:out value='${userInfo.userName}' />"
                       required maxlength="100" autocomplete="name"
                       placeholder="이름을 입력하세요">
            </div>

            <div class="form-actions ui-form-actions">
                <button type="submit"
                        class="ui-button button--primary button--md"
                        data-busy-label="저장 중">
                    <i class="fas fa-save"></i>
                    저장
                </button>
                <a href="${pageContext.request.contextPath}/mypage"
                   class="ui-button button--secondary button--md">
                    <i class="fas fa-times"></i>
                    취소
                </a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/includes/footer.jsp" %>
