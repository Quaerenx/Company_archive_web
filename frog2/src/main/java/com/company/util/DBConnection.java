package com.company.util;

import com.company.config.ApplicationEnvironment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * HikariCP Connection Pool을 사용하는 데이터베이스 연결 유틸리티
 */
public class DBConnection {
    private static final String CONFIG_PROPERTY = "frog2.config";
    private static final String QUERY_TIMEOUT_PROPERTY =
            "jdbc.queryTimeoutSeconds";
    private static final String[] REQUIRED_PROPERTIES = { "db.url", "db.user", "db.password", "db.driver" };
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;
    private static int queryTimeoutSeconds;
    
    static {
        try {
            initializeDataSource();
            logger.info("HikariCP Connection Pool 초기화 완료");
        } catch (Exception e) {
            logger.error("DB Connection Pool 초기화 실패", e);
            throw new RuntimeException("DB Connection Pool 초기화 실패", e);
        }
    }
    
    /**
     * HikariCP DataSource 초기화
     */
    private static void initializeDataSource() throws IOException {
        Properties props = loadProperties();
        queryTimeoutSeconds = positiveIntProperty(
                props, QUERY_TIMEOUT_PROPERTY, 30);
        
        HikariConfig config = new HikariConfig();
        
        // DB 연결 정보
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));
        config.setDriverClassName(props.getProperty("db.driver"));
        config.setReadOnly(ApplicationEnvironment.isReadOnly());
        
        // Connection Pool 설정
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("hikari.maximumPoolSize", "20")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("hikari.minimumIdle", "5")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("hikari.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("hikari.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("hikari.maxLifetime", "1800000")));
        
        // 연결 검증
        config.setConnectionTestQuery("SELECT 1");
        
        // Pool 이름 설정
        config.setPoolName("FrogDB-Pool");
        
        // 성능 최적화 설정
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Leak Detection (개발 환경용 - 운영에서는 비활성화 권장)
        config.setLeakDetectionThreshold(60000); // 60초
        
        dataSource = new HikariDataSource(config);
        
        logger.info(
                "Connection Pool 설정 완료 - MaxPoolSize: {}, MinIdle: {}, ReadOnly: {}, QueryTimeoutSeconds: {}",
                config.getMaximumPoolSize(),
                config.getMinimumIdle(),
                config.isReadOnly(),
                queryTimeoutSeconds);
    }

    private static int positiveIntProperty(
            Properties properties,
            String name,
            int defaultValue) throws IOException {
        try {
            int value = Integer.parseInt(
                    properties.getProperty(name, Integer.toString(defaultValue)));
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Database configuration key must be a positive integer: " + name,
                    exception);
        }
    }

    private static Properties loadProperties() throws IOException {
        String configuredPath = System.getProperty(CONFIG_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IOException("JVM property " + CONFIG_PROPERTY + " must point to an external db.properties file");
        }

        Path configPath = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(configPath)) {
            throw new IOException("External database configuration file does not exist");
        }

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(configPath)) {
            props.load(is);
        }
        for (String requiredProperty : REQUIRED_PROPERTIES) {
            if (props.getProperty(requiredProperty) == null || props.getProperty(requiredProperty).isBlank()) {
                throw new IOException("Missing required database configuration key: " + requiredProperty);
            }
        }
        logger.debug("External database configuration loaded");
        return props;
    }
    
    /**
     * Connection Pool에서 연결을 가져옵니다.
     * @return Database Connection
     * @throws SQLException 연결 실패 시
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource가 초기화되지 않았습니다.");
        }
        
        Connection conn = JdbcConnectionDecorator.decorate(
                dataSource.getConnection(),
                queryTimeoutSeconds,
                ApplicationEnvironment.isReadOnly());
        if (logger.isDebugEnabled()) {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            logger.debug("Connection 획득 - Active: {}, Idle: {}, Total: {}",
                    pool.getActiveConnections(),
                    pool.getIdleConnections(),
                    pool.getTotalConnections());
        }
        
        return conn;
    }
    
    /**
     * 리소스 해제 메소드 (Connection, Statement, ResultSet 등)
     */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                    logger.trace("리소스 종료: {}", resource.getClass().getSimpleName());
                } catch (Exception e) {
                    logger.warn("리소스 종료 중 오류 발생: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Connection Pool 통계 정보 조회 (모니터링용)
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        
        return String.format(
            "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    /**
     * 애플리케이션 종료 시 Connection Pool 정리
     * ServletContextListener에서 호출해야 합니다.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Connection Pool 종료 시작...");
            logger.info("최종 통계: {}", getPoolStats());
            dataSource.close();
            dataSource = null;
            logger.info("Connection Pool 종료 완료");
        }
        try {
            int deregistered = JdbcDriverLifecycle.deregisterDrivers(
                    DBConnection.class.getClassLoader());
            logger.info("JDBC Driver 해제 완료 - Count: {}", deregistered);
        } catch (SQLException exception) {
            logger.warn("JDBC Driver 해제 중 오류가 발생했습니다.", exception);
        }
    }
}
