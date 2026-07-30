package com.company.listener;

import com.company.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 애플리케이션 생명주기 관리 리스너
 * - 애플리케이션 시작 시: 초기화 로그
 * - 애플리케이션 종료 시: Connection Pool 정리
 */
public class AppLifecycleListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppLifecycleListener.class);
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("애플리케이션 시작 - Frog2 System");
        logger.info("Context Path: {}", sce.getServletContext().getContextPath());
        logger.info("========================================");
        
        // Connection Pool 통계 출력
        logger.info("Connection Pool 상태: {}", DBConnection.getPoolStats());
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("애플리케이션 종료 시작...");
        logger.info("========================================");
        
        // Connection Pool 종료
        DBConnection.shutdown();
        
        logger.info("애플리케이션 종료 완료");
    }
}
