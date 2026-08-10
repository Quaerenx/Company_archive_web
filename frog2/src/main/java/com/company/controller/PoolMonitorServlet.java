package com.company.controller;

import com.company.model.UserDTO;
import com.company.security.AdminAccessPolicy;
import com.company.security.SessionPrincipal;
import com.company.util.DBConnection;
import com.company.web.ApplicationError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection Pool 상태 모니터링 Servlet
 * URL: /admin/pool-status
 */
public class PoolMonitorServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PoolMonitorServlet.class);
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "Authentication is required");
            return;
        }
        if (!AdminAccessPolicy.isAdmin(user)) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "admin_access_required",
                    "Administrator access is required");
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html><head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Connection Pool Monitor</title>");
        out.println("<link rel='stylesheet' href='"
                + request.getContextPath()
                + "/resources/css/tokens.css'>");
        out.println("<link rel='stylesheet' href='"
                + request.getContextPath()
                + "/resources/css/base.css'>");
        out.println("<link rel='stylesheet' href='"
                + request.getContextPath()
                + "/resources/css/pages/pool_monitor.css'>");
        out.println("</head><body>");
        out.println("<div class='container'>");
        out.println("<h1>🔧 HikariCP Connection Pool 모니터</h1>");
        
        
        // Connection 테스트
        out.println("<h2>🔌 연결 테스트</h2>");
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = DBConnection.getConnection()) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            out.println("<div class='test-result success'>");
            out.println("✅ <strong>데이터베이스 연결 정상</strong>");
            out.println("</div>");
            
            logger.info("Pool 모니터 - 연결 테스트 성공 ({}ms)", elapsed);
            
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            out.println("<div class='test-result error'>");
            out.println("❌ <strong>데이터베이스 연결을 확인할 수 없습니다.</strong>");
            out.println("</div>");
            
            logger.error("Pool 모니터 - 연결 테스트 실패", e);
        }
        
        // 새로고침 버튼
        out.println("<br><button type='button' class='refresh-btn' id='pool-refresh'>🔄 새로고침</button>");
        
        out.println("<p class='last-updated'>");
        out.println("마지막 갱신: " + new java.util.Date());
        out.println("</p>");
        
        out.println("</div>");
        out.println("<script src='" + request.getContextPath()
                + "/resources/js/pages/pool_monitor.js'></script>");
        out.println("</body></html>");
    }
}
