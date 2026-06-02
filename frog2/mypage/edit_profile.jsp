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
    
    UserDTO userInfo = (UserDTO) request.getAttribute("userInfo");
%>
<c:set var="pageTitle" value="프로필 수정" scope="request" />

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
        .edit-container {
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
        
        .form-control:disabled {
            background-color: #F7F7F7;
            color: #666666;
            cursor: not-allowed;
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
    </style>
</head>
<body class="page-customers">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<div class="edit-container">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-user-edit"></i> 프로필 수정
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            사용자 정보 수정
        </jsp:attribute>
    </t:pageHeader>
    
    <div class="form-card">
        <form action="${pageContext.request.contextPath}/mypage" method="post" onsubmit="return validateForm()">
            <input type="hidden" name="formAction" value="updateProfile">
            
            <div class="form-group">
                <label class="form-label" for="userId">
                    <i class="fas fa-id-card"></i> 아이디
                </label>
                <input type="text" class="form-control" id="userId" name="userId" 
                       value="<%= userInfo != null ? userInfo.getUserId() : "" %>" disabled>
                <div class="help-text">아이디는 변경할 수 없습니다.</div>
            </div>
            
            <div class="form-group">
                <label class="form-label" for="userName">
                    <i class="fas fa-user"></i> 이름 <span class="required">*</span>
                </label>
                <input type="text" class="form-control" id="userName" name="userName" 
                       value="<%= userInfo != null ? userInfo.getUserName() : "" %>" 
                       required placeholder="이름을 입력하세요">
            </div>
            
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">
                    <i class="fas fa-save"></i>
                    저장
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
    function validateForm() {
        var userName = document.getElementById('userName').value.trim();
        
        if (userName === '') {
            alert('이름을 입력해주세요.');
            document.getElementById('userName').focus();
            return false;
        }
        
        return confirm('프로필을 수정하시겠습니까?');
    }
</script>
</body>
</html>
