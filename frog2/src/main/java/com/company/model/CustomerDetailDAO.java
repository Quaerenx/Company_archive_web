package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerDetailDAO {
    private static final int SAVE_LOCK_STRIPES = 64;
    private static final ReentrantLock[] SAVE_LOCKS = createSaveLocks();
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();
    private static final String COLUMNS =
            "customer_name, system_name, customer_manager, si_company, si_manager, creator, create_date, "
                    + "main_manager, sub_manager, install_date, introduction_year, db_name, db_mode, "
                    + "vertica_version, license_info, said, node_count, vertica_admin, subcluster_yn, "
                    + "mc_yn, mc_host, mc_version, mc_admin, backup_yn, custom_resource_pool_yn, "
                    + "backup_note, os_info, memory_info, swap_memory, infra_type, cpu_socket, hyper_threading, "
                    + "cpu_core, data_area, depot_area, catalog_area, object_area, public_yn, "
                    + "public_network, private_yn, private_network, storage_yn, storage_network, "
                    + "etl_tool, bi_tool, db_encryption, cdc_tool, eos_date, customer_type, note";
    private static final String INSERT_PLACEHOLDERS = "?, ".repeat(49) + "?";
    private static final String UPDATE_ASSIGNMENTS =
            "system_name = ?, customer_manager = ?, si_company = ?, si_manager = ?, creator = ?, "
                    + "create_date = ?, main_manager = ?, sub_manager = ?, install_date = ?, "
                    + "introduction_year = ?, db_name = ?, db_mode = ?, vertica_version = ?, "
                    + "license_info = ?, said = ?, node_count = ?, vertica_admin = ?, "
                    + "subcluster_yn = ?, mc_yn = ?, mc_host = ?, mc_version = ?, mc_admin = ?, "
                    + "backup_yn = ?, custom_resource_pool_yn = ?, backup_note = ?, os_info = ?, "
                    + "memory_info = ?, swap_memory = ?, infra_type = ?, cpu_socket = ?, hyper_threading = ?, "
                    + "cpu_core = ?, data_area = ?, depot_area = ?, catalog_area = ?, object_area = ?, "
                    + "public_yn = ?, public_network = ?, private_yn = ?, private_network = ?, "
                    + "storage_yn = ?, storage_network = ?, etl_tool = ?, bi_tool = ?, "
                    + "db_encryption = ?, cdc_tool = ?, eos_date = ?, customer_type = ?, note = ?";

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    static List<String> requiredColumnNames() {
        return List.of(COLUMNS.split(", "));
    }

    public CustomerDetailDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    CustomerDetailDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new SchemaCapabilityCache());
    }

    CustomerDetailDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
    }

    public CustomerDetailDTO getCustomerDetail(String customerName) {
        return getCustomerDetail(CustomerDetailEnvironment.PROD, customerName);
    }

    public CustomerDetailDTO getCustomerDetailStg(String customerName) {
        return getCustomerDetail(CustomerDetailEnvironment.STAGING, customerName);
    }

    public CustomerDetailDTO getCustomerDetailDev(String customerName) {
        return getCustomerDetail(CustomerDetailEnvironment.DEVELOPMENT, customerName);
    }

    public CustomerDetailSet getCustomerDetails(String customerName) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = CustomerAuditSupport.isAvailable(
                    connection, schemaCapabilities);
            String sql = "SELECT 'prod' AS detail_environment, " + COLUMNS
                    + auditProjection(auditAvailable)
                    + " FROM vertica_customer_detail "
                    + "WHERE customer_name = ? AND is_deleted = 1 "
                    + "UNION ALL SELECT 'stg' AS detail_environment, " + COLUMNS
                    + emptyAuditProjection()
                    + " FROM vertica_customer_detail_stg WHERE customer_name = ? "
                    + "UNION ALL SELECT 'dev' AS detail_environment, " + COLUMNS
                    + emptyAuditProjection()
                    + " FROM vertica_customer_detail_dev WHERE customer_name = ?";
            CustomerDetailDTO production = null;
            CustomerDetailDTO staging = null;
            CustomerDetailDTO development = null;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, customerName);
                statement.setString(2, customerName);
                statement.setString(3, customerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        CustomerDetailDTO detail = mapRowToDetail(resultSet);
                        switch (resultSet.getString("detail_environment")) {
                            case "prod" -> production = detail;
                            case "stg" -> staging = detail;
                            case "dev" -> development = detail;
                            default -> throw new SQLException(
                                    "Unexpected customer detail environment");
                        }
                    }
                }
            }
            return new CustomerDetailSet(production, staging, development);
        } catch (SQLException exception) {
            throw DataAccessException.from("load customer details", exception);
        }
    }

    public boolean saveOrUpdateCustomerDetail(CustomerDetailDTO detail) {
        return saveOrUpdateCustomerDetail(detail, null);
    }

    public boolean saveOrUpdateCustomerDetail(
            CustomerDetailDTO detail, String actorUserId) {
        return saveOrUpdate(
                CustomerDetailEnvironment.PROD, detail, actorUserId);
    }

    public boolean saveOrUpdateCustomerDetailStg(CustomerDetailDTO detail) {
        return saveOrUpdate(
                CustomerDetailEnvironment.STAGING, detail, null);
    }

    public boolean saveOrUpdateCustomerDetailDev(CustomerDetailDTO detail) {
        return saveOrUpdate(
                CustomerDetailEnvironment.DEVELOPMENT, detail, null);
    }


    private CustomerDetailDTO getCustomerDetail(
            CustomerDetailEnvironment environment, String customerName) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = environment == CustomerDetailEnvironment.PROD
                    && CustomerAuditSupport.isAvailable(
                            connection, schemaCapabilities);
            return find(
                    connection, environment, customerName, auditAvailable);
        } catch (SQLException exception) {
            throw DataAccessException.from("load customer detail", exception);
        }
    }

    private CustomerDetailDTO find(
            Connection connection,
            CustomerDetailEnvironment environment,
            String customerName,
            boolean auditAvailable) throws SQLException {
        String sql = "SELECT " + COLUMNS
                + auditProjection(auditAvailable)
                + " FROM " + environment.tableName()
                + " WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRowToDetail(resultSet) : null;
            }
        }
    }

    private boolean saveOrUpdate(
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail,
            String actorUserId) {
        Objects.requireNonNull(detail, "detail");
        String customerName = Objects.requireNonNull(
                detail.getCustomerName(), "detail.customerName");
        ReentrantLock saveLock = saveLock(environment, customerName);
        saveLock.lock();
        try {
            try {
                return saveInTransaction(
                        environment, detail, actorUserId, true);
            } catch (SQLException exception) {
                if (!isDuplicateKey(exception)) {
                    throw DataAccessException.from(
                            "save customer detail", exception);
                }
                try {
                    return saveInTransaction(
                            environment, detail, actorUserId, false);
                } catch (SQLException retryFailure) {
                    retryFailure.addSuppressed(exception);
                    throw DataAccessException.from(
                            "save customer detail after concurrent insert",
                            retryFailure);
                }
            }
        } finally {
            saveLock.unlock();
        }
    }

    private static ReentrantLock saveLock(
            CustomerDetailEnvironment environment, String customerName) {
        int hash = Objects.hash(environment, customerName);
        return SAVE_LOCKS[Math.floorMod(hash, SAVE_LOCKS.length)];
    }

    private static ReentrantLock[] createSaveLocks() {
        ReentrantLock[] locks = new ReentrantLock[SAVE_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    private boolean saveInTransaction(
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail,
            String actorUserId,
            boolean insertWhenMissing) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = environment == CustomerDetailEnvironment.PROD
                    && CustomerAuditSupport.shouldAuditWrite(
                            connection, schemaCapabilities, actorUserId);
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable primaryFailure = null;
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                }

                boolean changed = update(
                        connection,
                        environment,
                        detail,
                        actorUserId,
                        auditAvailable);
                if (!changed && insertWhenMissing) {
                    changed = insert(
                            connection,
                            environment,
                            detail,
                            actorUserId,
                            auditAvailable);
                }
                connection.commit();
                return changed;
            } catch (SQLException | RuntimeException exception) {
                primaryFailure = exception;
                rollback(connection, exception);
                throw exception;
            } finally {
                if (originalAutoCommit) {
                    restoreAutoCommit(connection, primaryFailure);
                }
            }
        }
    }

    private static boolean isDuplicateKey(SQLException exception) {
        for (SQLException current = exception;
                current != null;
                current = current.getNextException()) {
            if ("23505".equals(current.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private boolean insert(
            Connection connection,
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail,
            String actorUserId,
            boolean auditAvailable) throws SQLException {
        String columns = COLUMNS;
        String values = INSERT_PLACEHOLDERS;
        if (auditAvailable) {
            columns += ", updated_at, updated_by";
            values += ", CURRENT_TIMESTAMP, ?";
        }
        String sql = "INSERT INTO " + environment.tableName() + " (" + columns
                + ") VALUES (" + values + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, detail.getCustomerName());
            int nextIndex = bindMutableFields(statement, detail, 2);
            requireNextIndex(nextIndex);
            if (auditAvailable) {
                statement.setString(nextIndex, actorUserId.trim());
            }
            return statement.executeUpdate() > 0;
        }
    }

    private boolean update(
            Connection connection,
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail,
            String actorUserId,
            boolean auditAvailable) throws SQLException {
        String assignments = UPDATE_ASSIGNMENTS;
        if (auditAvailable) {
            assignments += ", updated_at = CURRENT_TIMESTAMP, updated_by = ?";
        }
        String sql = "UPDATE " + environment.tableName() + " SET " + assignments
                + " WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindMutableFields(statement, detail, 1);
            if (auditAvailable) {
                statement.setString(nextIndex++, actorUserId.trim());
            }
            statement.setString(nextIndex, detail.getCustomerName());
            requireNextIndex(nextIndex + (auditAvailable ? 0 : 1));
            return statement.executeUpdate() > 0;
        }
    }

    private static int bindMutableFields(
            PreparedStatement statement, CustomerDetailDTO detail, int startIndex)
            throws SQLException {
        int index = startIndex;
        statement.setString(index++, detail.getSystemName());
        statement.setString(index++, detail.getCustomerManager());
        statement.setString(index++, detail.getSiCompany());
        statement.setString(index++, detail.getSiManager());
        statement.setString(index++, detail.getCreator());
        setTimestamp(statement, index++, detail.getCreateDate());
        statement.setString(index++, detail.getMainManager());
        statement.setString(index++, detail.getSubManager());
        setTimestamp(statement, index++, detail.getInstallDate());
        statement.setString(index++, detail.getIntroductionYear());
        statement.setString(index++, detail.getDbName());
        statement.setString(index++, detail.getDbMode());
        statement.setString(index++, detail.getVerticaVersion());
        statement.setString(index++, detail.getLicenseInfo());
        statement.setString(index++, detail.getSaid());
        statement.setString(index++, detail.getNodeCount());
        statement.setString(index++, detail.getVerticaAdmin());
        statement.setString(index++, detail.getSubclusterYn());
        statement.setString(index++, detail.getMcYn());
        statement.setString(index++, detail.getMcHost());
        statement.setString(index++, detail.getMcVersion());
        statement.setString(index++, detail.getMcAdmin());
        statement.setString(index++, detail.getBackupYn());
        statement.setString(index++, detail.getCustomResourcePoolYn());
        statement.setString(index++, detail.getBackupNote());
        statement.setString(index++, detail.getOsInfo());
        statement.setString(index++, detail.getMemoryInfo());
        statement.setString(index++, detail.getSwapMemory());
        statement.setString(index++, detail.getInfraType());
        statement.setString(index++, detail.getCpuSocket());
        statement.setString(index++, detail.getHyperThreading());
        statement.setString(index++, detail.getCpuCore());
        statement.setString(index++, detail.getDataArea());
        statement.setString(index++, detail.getDepotArea());
        statement.setString(index++, detail.getCatalogArea());
        statement.setString(index++, detail.getObjectArea());
        statement.setString(index++, detail.getPublicYn());
        statement.setString(index++, detail.getPublicNetwork());
        statement.setString(index++, detail.getPrivateYn());
        statement.setString(index++, detail.getPrivateNetwork());
        statement.setString(index++, detail.getStorageYn());
        statement.setString(index++, detail.getStorageNetwork());
        statement.setString(index++, detail.getEtlTool());
        statement.setString(index++, detail.getBiTool());
        statement.setString(index++, detail.getDbEncryption());
        statement.setString(index++, detail.getCdcTool());
        setTimestamp(statement, index++, detail.getEosDate());
        statement.setString(index++, detail.getCustomerType());
        statement.setString(index++, detail.getNote());
        return index;
    }

    private static void setTimestamp(
            PreparedStatement statement, int index, java.util.Date value) throws SQLException {
        statement.setTimestamp(index, value == null ? null : new Timestamp(value.getTime()));
    }

    private static void requireNextIndex(int nextIndex) {
        if (nextIndex != 51) {
            throw new IllegalStateException("Unexpected customer detail parameter count");
        }
    }

    private static void rollback(Connection connection, Throwable primaryFailure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            primaryFailure.addSuppressed(rollbackFailure);
        }
    }

    private static void restoreAutoCommit(Connection connection, Throwable primaryFailure)
            throws SQLException {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException restoreFailure) {
            if (primaryFailure != null) {
                primaryFailure.addSuppressed(restoreFailure);
            } else {
                throw restoreFailure;
            }
        }
    }

    private static CustomerDetailDTO mapRowToDetail(ResultSet resultSet) throws SQLException {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName(resultSet.getString("customer_name"));
        detail.setSystemName(resultSet.getString("system_name"));
        detail.setCustomerManager(resultSet.getString("customer_manager"));
        detail.setSiCompany(resultSet.getString("si_company"));
        detail.setSiManager(resultSet.getString("si_manager"));
        detail.setCreator(resultSet.getString("creator"));
        detail.setCreateDate(toDate(resultSet.getTimestamp("create_date")));
        detail.setMainManager(resultSet.getString("main_manager"));
        detail.setSubManager(resultSet.getString("sub_manager"));
        detail.setInstallDate(toDate(resultSet.getTimestamp("install_date")));
        detail.setIntroductionYear(resultSet.getString("introduction_year"));
        detail.setDbName(resultSet.getString("db_name"));
        detail.setDbMode(resultSet.getString("db_mode"));
        detail.setVerticaVersion(resultSet.getString("vertica_version"));
        detail.setLicenseInfo(resultSet.getString("license_info"));
        detail.setSaid(resultSet.getString("said"));
        detail.setNodeCount(resultSet.getString("node_count"));
        detail.setVerticaAdmin(resultSet.getString("vertica_admin"));
        detail.setSubclusterYn(resultSet.getString("subcluster_yn"));
        detail.setMcYn(resultSet.getString("mc_yn"));
        detail.setMcHost(resultSet.getString("mc_host"));
        detail.setMcVersion(resultSet.getString("mc_version"));
        detail.setMcAdmin(resultSet.getString("mc_admin"));
        detail.setBackupYn(resultSet.getString("backup_yn"));
        detail.setCustomResourcePoolYn(resultSet.getString("custom_resource_pool_yn"));
        detail.setBackupNote(resultSet.getString("backup_note"));
        detail.setOsInfo(resultSet.getString("os_info"));
        detail.setMemoryInfo(resultSet.getString("memory_info"));
        detail.setSwapMemory(resultSet.getString("swap_memory"));
        detail.setInfraType(resultSet.getString("infra_type"));
        detail.setCpuSocket(resultSet.getString("cpu_socket"));
        detail.setHyperThreading(resultSet.getString("hyper_threading"));
        detail.setCpuCore(resultSet.getString("cpu_core"));
        detail.setDataArea(resultSet.getString("data_area"));
        detail.setDepotArea(resultSet.getString("depot_area"));
        detail.setCatalogArea(resultSet.getString("catalog_area"));
        detail.setObjectArea(resultSet.getString("object_area"));
        detail.setPublicYn(resultSet.getString("public_yn"));
        detail.setPublicNetwork(resultSet.getString("public_network"));
        detail.setPrivateYn(resultSet.getString("private_yn"));
        detail.setPrivateNetwork(resultSet.getString("private_network"));
        detail.setStorageYn(resultSet.getString("storage_yn"));
        detail.setStorageNetwork(resultSet.getString("storage_network"));
        detail.setEtlTool(resultSet.getString("etl_tool"));
        detail.setBiTool(resultSet.getString("bi_tool"));
        detail.setDbEncryption(resultSet.getString("db_encryption"));
        detail.setCdcTool(resultSet.getString("cdc_tool"));
        detail.setEosDate(toDate(resultSet.getTimestamp("eos_date")));
        detail.setCustomerType(resultSet.getString("customer_type"));
        detail.setNote(resultSet.getString("note"));
        detail.setUpdatedAt(toDate(resultSet.getTimestamp("audit_updated_at")));
        detail.setUpdatedBy(resultSet.getString("audit_updated_by"));
        return detail;
    }

    private static String auditProjection(boolean auditAvailable) {
        return auditAvailable
                ? ", updated_at AS audit_updated_at, updated_by AS audit_updated_by"
                : emptyAuditProjection();
    }

    private static String emptyAuditProjection() {
        return ", CAST(NULL AS TIMESTAMP) AS audit_updated_at, "
                + "CAST(NULL AS VARCHAR(100)) AS audit_updated_by";
    }

    private static java.util.Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new java.util.Date(timestamp.getTime());
    }
}
