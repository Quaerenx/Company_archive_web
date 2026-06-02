<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.company.model.UserDTO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    UserDTO currentUser = (UserDTO) session.getAttribute("user");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    String message = (String) request.getAttribute("message");
    String messageType = (String) request.getAttribute("messageType");
%>
<c:set var="pageTitle" value="비밀번호 변경" scope="request" />

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main_style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/utilities.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/customers.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <style>
        .password-container {
            width: 100%;
            max-width: 1000px;
            margin: 0 auto;
            padding: 32px 16px;
        }
        
        .form-card {
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            max-width: 600px;
            margin: 0 auto;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        .form-label {
            display: block;
            font-weight: 600;
            margin-bottom: 8px;
            color: #333333;
            font-size: 0.875rem;
        }
        
        .form-control {
            width: 100%;
            padding: 10px 12px;
            border: 1px solid #DEDEDE;
            border-radius: 6px;
            font-size: 0.875rem;
            box-sizing: border-box;
            transition: border-color 0.2s;
        }
        
        .form-control:focus {
            outline: none;
            border-color: #3D5A80;
            box-shadow: 0 0 0 3px rgba(61, 90, 128, 0.1);
        }
        
        .form-actions {
            display: flex;
            gap: 10px;
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #E9E9E9;
        }
        
        .form-actions .btn {
            flex: 1;
        }
        
        .help-text {
            font-size: 0.813rem;
            color: #666666;
            margin-top: 4px;
        }
        
        .required {
            color: #dc3545;
        }
        
        .password-requirements {
            background-color: #EEF1F6;
            border: 1px solid #DEDEDE;
            padding: 16px;
            border-radius: 6px;
            margin-bottom: 24px;
        }
        
        .password-requirements h3 {
            margin: 0 0 12px 0;
            font-size: 0.875rem;
            color: #333333;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        
        .password-requirements ul {
            margin: 0;
            padding-left: 20px;
        }
        
        .password-requirements li {
            font-size: 0.813rem;
            color: #666666;
            margin-bottom: 6px;
        }
    </style>
</head>
<body class="page-customers">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<div class="password-container">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-key"></i> 비밀번호 변경
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            새로운 비밀번호로 변경
        </jsp:attribute>
    </t:pageHeader>
    
    <div class="form-card">
        <div class="password-requirements">
            <h3>
                <i class="fas fa-info-circle" style="color: #3D5A80;"></i>
                비밀번호 요구사항
            </h3>
            <ul>
                <li>최소 8자 이상</li>
                <li>영문, 숫자, 특수문자 조합 권장</li>
                <li>현재 비밀번호와 다른 비밀번호 사용</li>
            </ul>
        </div>
        
        <% if (message != null) { %>
            <div class="alert alert-<%= messageType %>">
                <i class="fas fa-<%= "success".equals(messageType) ? "check-circle" : "exclamation-circle" %>"></i>
                <%= message %>
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/mypage" method="post" onsubmit="return validatePassword()">
            <input type="hidden" name="formAction" value="updatePassword">
            
            <div class="form-group">
                <label class="form-label" for="currentPassword">
                    <i class="fas fa-lock"></i> 현재 비밀번호 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="currentPassword" name="currentPassword" 
                       required placeholder="현재 비밀번호를 입력하세요">
            </div>
            
            <div class="form-group">
                <label class="form-label" for="newPassword">
                    <i class="fas fa-lock-open"></i> 새 비밀번호 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="newPassword" name="newPassword" 
                       required placeholder="새 비밀번호를 입력하세요">
                <div class="help-text">최소 8자 이상 입력해주세요.</div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="confirmPassword">
                    <i class="fas fa-check-circle"></i> 새 비밀번호 확인 <span class="required">*</span>
                </label>
                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" 
                       required placeholder="새 비밀번호를 다시 입력하세요">
            </div>
            
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">
                    <i class="fas fa-save"></i>
                    변경
                </button>
                <a href="${pageContext.request.contextPath}/mypage" class="btn btn-secondary">
                    <i class="fas fa-times"></i>
                    취소
                </a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/includes/footer_content.jspf" %>

<script>
    function validatePassword() {
        var currentPassword = document.getElementById('currentPassword').value;
        var newPassword = document.getElementById('newPassword').value;
        var confirmPassword = document.getElementById('confirmPassword').value;
        
        if (currentPassword === '') {
            alert('현재 비밀번호를 입력해주세요.');
            document.getElementById('currentPassword').focus();
            return false;
        }
        
        if (newPassword.length < 8) {
            alert('새 비밀번호는 최소 8자 이상이어야 합니다.');
            document.getElementById('newPassword').focus();
            return false;
        }
        
        if (newPassword !== confirmPassword) {
            alert('새 비밀번호가 일치하지 않습니다.');
            document.getElementById('confirmPassword').focus();
            return false;
        }
        
        if (currentPassword === newPassword) {
            alert('현재 비밀번호와 새 비밀번호가 동일합니다.\n다른 비밀번호를 입력해주세요.');
            document.getElementById('newPassword').focus();
            return false;
        }
        
        return confirm('비밀번호를 변경하시겠습니까?');
    }
</script>
</body>
</html>
