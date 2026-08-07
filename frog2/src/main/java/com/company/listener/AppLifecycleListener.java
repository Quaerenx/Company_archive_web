package com.company.listener;

import com.company.model.DataAccessException;
import com.company.model.DatabaseSchemaReadiness;
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
    public static final String SCHEMA_READY_ATTRIBUTE = "frog2.schemaReady";
    public static final String SCHEMA_MISSING_ATTRIBUTE =
            "frog2.schemaMissingRequirements";
    private static final Logger logger = LoggerFactory.getLogger(AppLifecycleListener.class);
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("애플리케이션 시작 - Frog2 System");
        logger.info("Context Path: {}", sce.getServletContext().getContextPath());
        logger.info("========================================");
        
        // Connection Pool 통계 출력
        logger.info("Connection Pool 상태: {}", DBConnection.getPoolStats());

        try {
            DatabaseSchemaReadiness.Report report =
                    DatabaseSchemaReadiness.inspect();
            sce.getServletContext().setAttribute(
                    SCHEMA_READY_ATTRIBUTE, report.ready());
            sce.getServletContext().setAttribute(
                    SCHEMA_MISSING_ATTRIBUTE,
                    report.missingRequirements());
            if (report.ready()) {
                logger.info("Database schema readiness check passed");
            } else {
                logger.warn(
                        "Database schema readiness check failed: {}",
                        report.missingRequirements());
            }
        } catch (DataAccessException exception) {
            sce.getServletContext().setAttribute(
                    SCHEMA_READY_ATTRIBUTE, false);
            sce.getServletContext().setAttribute(
                    SCHEMA_MISSING_ATTRIBUTE, java.util.List.of());
            logger.error(
                    "Database schema readiness could not be inspected",
                    exception);
        }
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
