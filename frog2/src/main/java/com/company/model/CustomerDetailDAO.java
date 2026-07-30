package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

public class CustomerDetailDAO {
    private static final String COLUMNS =
            "customer_name, system_name, customer_manager, si_company, si_manager, creator, create_date, "
                    + "main_manager, sub_manager, install_date, introduction_year, db_name, db_mode, "
                    + "vertica_version, license_info, said, node_count, vertica_admin, subcluster_yn, "
                    + "mc_yn, mc_host, mc_version, mc_admin, backup_yn, custom_resource_pool_yn, "
                    + "backup_note, os_info, memory_info, infra_type, cpu_socket, hyper_threading, "
                    + "cpu_core, data_area, depot_area, catalog_area, object_area, public_yn, "
                    + "public_network, private_yn, private_network, storage_yn, storage_network, "
                    + "etl_tool, bi_tool, db_encryption, cdc_tool, eos_date, customer_type, note";
    private static final String INSERT_PLACEHOLDERS = "?, ".repeat(48) + "?";
    private static final String UPDATE_ASSIGNMENTS =
            "system_name = ?, customer_manager = ?, si_company = ?, si_manager = ?, creator = ?, "
                    + "create_date = ?, main_manager = ?, sub_manager = ?, install_date = ?, "
                    + "introduction_year = ?, db_name = ?, db_mode = ?, vertica_version = ?, "
                    + "license_info = ?, said = ?, node_count = ?, vertica_admin = ?, "
                    + "subcluster_yn = ?, mc_yn = ?, mc_host = ?, mc_version = ?, mc_admin = ?, "
                    + "backup_yn = ?, custom_resource_pool_yn = ?, backup_note = ?, os_info = ?, "
                    + "memory_info = ?, infra_type = ?, cpu_socket = ?, hyper_threading = ?, "
                    + "cpu_core = ?, data_area = ?, depot_area = ?, catalog_area = ?, object_area = ?, "
                    + "public_yn = ?, public_network = ?, private_yn = ?, private_network = ?, "
                    + "storage_yn = ?, storage_network = ?, etl_tool = ?, bi_tool = ?, "
                    + "db_encryption = ?, cdc_tool = ?, eos_date = ?, customer_type = ?, note = ?";

    private final JdbcConnectionProvider connectionProvider;

    public CustomerDetailDAO() {
        this(DBConnection::getConnection);
    }

    CustomerDetailDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
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
            return new CustomerDetailSet(
                    find(connection, CustomerDetailEnvironment.PROD, customerName),
                    find(connection, CustomerDetailEnvironment.STAGING, customerName),
                    find(connection, CustomerDetailEnvironment.DEVELOPMENT, customerName));
        } catch (SQLException exception) {
            throw DataAccessException.from("load customer details", exception);
        }
    }

    public boolean saveOrUpdateCustomerDetail(CustomerDetailDTO detail) {
        return saveOrUpdate(CustomerDetailEnvironment.PROD, detail);
    }

    public boolean saveOrUpdateCustomerDetailStg(CustomerDetailDTO detail) {
        return saveOrUpdate(CustomerDetailEnvironment.STAGING, detail);
    }

    public boolean saveOrUpdateCustomerDetailDev(CustomerDetailDTO detail) {
        return saveOrUpdate(CustomerDetailEnvironment.DEVELOPMENT, detail);
    }


    private CustomerDetailDTO getCustomerDetail(
            CustomerDetailEnvironment environment, String customerName) {
        try (Connection connection = connectionProvider.getConnection()) {
            return find(connection, environment, customerName);
        } catch (SQLException exception) {
            throw DataAccessException.from("load customer detail", exception);
        }
    }

    private CustomerDetailDTO find(
            Connection connection,
            CustomerDetailEnvironment environment,
            String customerName) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM " + environment.tableName()
                + " WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRowToDetail(resultSet) : null;
            }
        }
    }

    private boolean saveOrUpdate(
            CustomerDetailEnvironment environment, CustomerDetailDTO detail) {
        Objects.requireNonNull(detail, "detail");
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable primaryFailure = null;
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                }

                boolean exists = exists(connection, environment, detail.getCustomerName());
                boolean changed = exists
                        ? update(connection, environment, detail)
                        : insert(connection, environment, detail);
                if (originalAutoCommit) {
                    connection.commit();
                }
                return changed;
            } catch (SQLException | RuntimeException exception) {
                primaryFailure = exception;
                if (originalAutoCommit) {
                    rollback(connection, exception);
                }
                throw exception;
            } finally {
                if (originalAutoCommit) {
                    restoreAutoCommit(connection, primaryFailure);
                }
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("save customer detail", exception);
        }
    }

    private boolean exists(
            Connection connection,
            CustomerDetailEnvironment environment,
            String customerName) throws SQLException {
        String sql = "SELECT 1 FROM " + environment.tableName() + " WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean insert(
            Connection connection,
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail) throws SQLException {
        String sql = "INSERT INTO " + environment.tableName() + " (" + COLUMNS
                + ") VALUES (" + INSERT_PLACEHOLDERS + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, detail.getCustomerName());
            int nextIndex = bindMutableFields(statement, detail, 2);
            requireNextIndex(nextIndex);
            return statement.executeUpdate() > 0;
        }
    }

    private boolean update(
            Connection connection,
            CustomerDetailEnvironment environment,
            CustomerDetailDTO detail) throws SQLException {
        String sql = "UPDATE " + environment.tableName() + " SET " + UPDATE_ASSIGNMENTS
                + " WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextIndex = bindMutableFields(statement, detail, 1);
            statement.setString(nextIndex, detail.getCustomerName());
            requireNextIndex(nextIndex + 1);
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
        if (nextIndex != 50) {
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
        return detail;
    }

    private static java.util.Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new java.util.Date(timestamp.getTime());
    }
}