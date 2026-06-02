<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.company.model.UserDTO" %>
<%@ page import="com.company.model.MaintenanceRecordDTO" %>
<%@ page import="com.company.model.TroubleshootingDTO" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    UserDTO currentUser = (UserDTO) session.getAttribute("user");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    
    UserDTO userInfo = (UserDTO) request.getAttribute("userInfo");
    List<MaintenanceRecordDTO> myMaintenanceRecords = (List<MaintenanceRecordDTO>) request.getAttribute("myMaintenanceRecords");
    List<TroubleshootingDTO> myTroubleshootings = (List<TroubleshootingDTO>) request.getAttribute("myTroubleshootings");
    Integer maintenanceCount = (Integer) request.getAttribute("maintenanceCount");
    Integer troubleshootingCount = (Integer) request.getAttribute("troubleshootingCount");
    
    String message = (String) request.getAttribute("message");
    String messageType = (String) request.getAttribute("messageType");
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
%>
<c:set var="pageTitle" value="마이페이지" scope="request" />

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/pages/dashboard_box.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css">
    <style>
        .mypage-container {
            width: 100%;
            max-width: 1000px;
            margin: 0 auto;
            padding: 32px 16px;
        }
        
        .profile-card {
            background: white;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        
        .profile-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 12px;
            border-bottom: 2px solid #e5e7eb;
        }
        
        .profile-header h2 {
            margin: 0;
            font-size: 1.25rem;
            color: #333333;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .profile-actions {
            display: flex;
            gap: 8px;
        }
        
        .profile-info {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
        }
        
        .info-item {
            padding: 12px;
            background: #F7F7F7;
            border-radius: 6px;
            border: 1px solid #E9E9E9;
        }
        
        .info-label {
            font-size: 0.875rem;
            font-weight: 600;
            color: #666666;
            margin-bottom: 4px;
        }
        
        .info-value {
            font-size: 1rem;
            color: #333333;
            font-weight: 500;
        }
        
        .stats-section {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }
        
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            text-align: center;
            border-top: 3px solid #3D5A80;
        }
        
        .stat-number {
            font-size: 2.5rem;
            font-weight: bold;
            color: #3D5A80;
            margin: 8px 0;
        }
        
        .stat-label {
            font-size: 0.875rem;
            color: #666666;
            font-weight: 500;
        }
        
        .quick-links-section {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        
        .quick-links-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 2px solid #E9E9E9;
        }
        
        .quick-links-header h2 {
            margin: 0;
            font-size: 1.125rem;
            color: #333333;
        }
        
        .quick-links-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 12px;
        }
        
        .quick-link-item {
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 12px 16px;
            background: white;
            border: 2px solid #E9E9E9;
            border-radius: 8px;
            text-decoration: none;
            color: #333333;
            transition: all 0.3s;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
        }
        
        .quick-link-item:hover {
            border-color: #3D5A80;
            background-color: #F7F9FB;
            transform: translateY(-2px);
            box-shadow: 0 3px 6px rgba(0,0,0,0.12);
        }
        
        .quick-link-icon {
            font-size: 1.25rem;
            margin-right: 10px;
            color: #3D5A80;
        }
        
        .quick-link-content {
            flex: 1;
        }
        
        .quick-link-title {
            font-size: 0.938rem;
            font-weight: 600;
            color: #333333;
        }
        
        .quick-link-desc {
            font-size: 0.875rem;
            opacity: 0.9;
        }
        
        .activity-card {
            background: white;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        
        .activity-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 2px solid #E9E9E9;
        }
        
        .activity-header h2 {
            margin: 0;
            font-size: 1.125rem;
            color: #333333;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .activity-list {
            max-height: 400px;
            overflow-y: auto;
        }
        
        .activity-item {
            padding: 12px;
            border-bottom: 1px solid #E9E9E9;
            transition: background-color 0.2s;
        }
        
        .activity-item:last-child {
            border-bottom: none;
        }
        
        .activity-item:hover {
            background-color: #F7F7F7;
        }
        
        .activity-title {
            font-weight: 600;
            color: #333333;
            margin-bottom: 8px;
            padding: 0 4px;
        }
        
        .activity-title a {
            color: #3D5A80;
            text-decoration: none;
        }
        
        .activity-title a:hover {
            text-decoration: underline;
        }
        
        .activity-meta {
            font-size: 0.875rem;
            color: #666666;
            margin-top: 8px;
            padding: 0 4px;
        }
        
        .empty-state {
            text-align: center;
            padding: 40px 20px;
            color: #999999;
        }
        
        .empty-state i {
            font-size: 3rem;
            margin-bottom: 16px;
            opacity: 0.5;
        }

        @media (max-width: 768px) {
            .profile-info {
                grid-template-columns: 1fr;
            }
            
            .stats-section {
                grid-template-columns: 1fr;
            }
            
            .profile-actions {
                flex-direction: column;
                width: 100%;
            }
            
            .profile-header {
                flex-direction: column;
                align-items: flex-start;
                gap: 12px;
            }
        }
    </style>
</head>
<body class="page-customers">
<%@ include file="/WEB-INF/includes/header_nav.jspf" %>

<div class="mypage-container">
    <t:pageHeader>
        <jsp:attribute name="title">
            <i class="fas fa-user"></i> 마이페이지
        </jsp:attribute>
        <jsp:attribute name="subtitle">
            내 정보 및 활동 현황
        </jsp:attribute>
    </t:pageHeader>
    
    <!-- 성공/에러 메시지 표시 -->
    <% if (message != null) { %>
        <div class="alert alert-<%= messageType %>">
            <i class="fas fa-<%= "success".equals(messageType) ? "check-circle" : "exclamation-circle" %>"></i>
            <%= message %>
        </div>
    <% } %>
    
    <!-- 프로필 카드 -->
    <div class="profile-card">
        <div class="profile-header">
            <h2>
                <i class="fas fa-user-circle"></i>
                내 정보
            </h2>
            <div class="profile-actions">
                <a href="mypage?action=editProfile" class="btn btn-primary btn-sm">
                    <i class="fas fa-edit"></i>
                    프로필 수정
                </a>
                <a href="mypage?action=changePassword" class="btn btn-secondary btn-sm">
                    <i class="fas fa-key"></i>
                    비밀번호 변경
                </a>
            </div>
        </div>
        
        <div class="profile-info">
            <div class="info-item">
                <div class="info-label">아이디</div>
                <div class="info-value"><%= userInfo != null ? userInfo.getUserId() : "" %></div>
            </div>
            <div class="info-item">
                <div class="info-label">이름</div>
                <div class="info-value"><%= userInfo != null ? userInfo.getUserName() : "" %></div>
            </div>
        </div>
    </div>
    
    <!-- 통계 카드 -->
    <div class="stats-section">
        <div class="stat-card">
            <i class="fas fa-clipboard-check" style="font-size: 2rem; color: #3D5A80; margin-bottom: 8px;"></i>
            <div class="stat-label">작성한 점검 기록</div>
            <div class="stat-number"><%= maintenanceCount != null ? maintenanceCount : 0 %></div>
        </div>
        <div class="stat-card">
            <i class="fas fa-tools" style="font-size: 2rem; color: #3D5A80; margin-bottom: 8px;"></i>
            <div class="stat-label">작성한 트러블슈팅</div>
            <div class="stat-number"><%= troubleshootingCount != null ? troubleshootingCount : 0 %></div>
        </div>
    </div>
    
    <!-- 바로가기 섹션 -->
    <div class="quick-links-section">
        <div class="quick-links-header">
            <h2>
                <i class="fas fa-th"></i>
                자주 사용하는 바로가기
            </h2>
        </div>
        <div class="quick-links-grid">
            <a href="<%= request.getContextPath() %>/mypage?action=monthlyResponse" class="quick-link-item">
                <i class="fas fa-calendar-alt quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">월별 고객 응대 현황</div>
                </div>
            </a>
            <a href="<%= request.getContextPath() %>/maintenance" class="quick-link-item">
                <i class="fas fa-clipboard-check quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">점검 기록 관리</div>
                </div>
            </a>
            <a href="<%= request.getContextPath() %>/troubleshooting" class="quick-link-item">
                <i class="fas fa-wrench quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">트러블슈팅 관리</div>
                </div>
            </a>
            <a href="<%= request.getContextPath() %>/customers" class="quick-link-item">
                <i class="fas fa-users quick-link-icon"></i>
                <div class="quick-link-content">
                    <div class="quick-link-title">고객사 관리</div>
                </div>
            </a>
        </div>
    </div>
    
    <!-- 최근 점검 기록 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-clipboard-list"></i>
                최근 작성한 점검 기록
            </h2>
            <a href="<%= request.getContextPath() %>/maintenance" class="btn btn-primary btn-sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <% 
            if (myMaintenanceRecords != null && !myMaintenanceRecords.isEmpty()) {
                int count = 0;
                for (MaintenanceRecordDTO record : myMaintenanceRecords) {
                    if (count >= 10) break;
            %>
                <div class="activity-item">
                    <div class="activity-title">
                        <a href="<%= request.getContextPath() %>/maintenance?view=view&id=<%= record.getMaintenanceId() %>">
                            <i class="fas fa-building"></i>
                            <%= record.getCustomerName() %>
                        </a>
                    </div>
                    <div class="activity-meta">
                        <i class="far fa-calendar-alt"></i> <%= record.getInspectionDate() != null ? sdf.format(record.getInspectionDate()) : "날짜 없음" %>
                        <span style="margin: 0 8px;">|</span>
                        <i class="fas fa-server"></i> Vertica <%= record.getVerticaVersion() != null ? record.getVerticaVersion() : "미기재" %>
                    </div>
                </div>
            <%
                    count++;
                }
            } else {
            %>
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>작성한 점검 기록이 없습니다.</p>
                </div>
            <% } %>
        </div>
    </div>
    
    <!-- 최근 트러블슈팅 -->
    <div class="activity-card">
        <div class="activity-header">
            <h2>
                <i class="fas fa-wrench"></i>
                최근 작성한 트러블슈팅
            </h2>
            <a href="<%= request.getContextPath() %>/troubleshooting" class="btn btn-primary btn-sm">
                <i class="fas fa-list"></i>
                전체 보기
            </a>
        </div>
        <div class="activity-list">
            <% 
            if (myTroubleshootings != null && !myTroubleshootings.isEmpty()) {
                int count = 0;
                for (TroubleshootingDTO ts : myTroubleshootings) {
                    if (count >= 10) break;
            %>
                <div class="activity-item">
                    <div class="activity-title">
                        <a href="<%= request.getContextPath() %>/troubleshooting?view=view&id=<%= ts.getId() %>">
                            <i class="fas fa-file-alt"></i>
                            <%= ts.getTitle() %>
                        </a>
                    </div>
                    <div class="activity-meta">
                        <i class="fas fa-building"></i> <%= ts.getCustomerName() %>
                        <span style="margin: 0 8px;">|</span>
                        <i class="far fa-calendar-alt"></i> 발생일: <%= ts.getOccurrenceDate() != null ? sdf.format(ts.getOccurrenceDate()) : "미기재" %>
                    </div>
                </div>
            <%
                    count++;
                }
            } else {
            %>
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <p>작성한 트러블슈팅이 없습니다.</p>
                </div>
            <% } %>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/includes/footer_content.jspf" %>
</body>
</html>
