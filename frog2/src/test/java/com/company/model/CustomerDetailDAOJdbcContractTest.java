package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLNonTransientException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CustomerDetailDAOJdbcContractTest {
    @Test
    void groupedReadUsesOneConnectionAndOneBoundedEnvironmentQuery() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(
                PaginationJdbcFixture.row(
                        "detail_environment", "prod",
                        "customer_name", "Acme",
                        "vertica_version", "12.0"),
                PaginationJdbcFixture.row(
                        "detail_environment", "stg",
                        "customer_name", "Acme",
                        "vertica_version", "11.1"),
                PaginationJdbcFixture.row(
                        "detail_environment", "dev",
                        "customer_name", "Acme",
                        "vertica_version", "10.0"));
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);

        CustomerDetailSet details = dao.getCustomerDetails("Acme");

        assertEquals("12.0", details.production().getVerticaVersion());
        assertEquals("11.1", details.staging().getVerticaVersion());
        assertEquals("10.0", details.development().getVerticaVersion());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "'prod' AS detail_environment"));
        assertTrue(statement.sql.contains(
                "FROM vertica_customer_detail WHERE customer_name = ? AND is_deleted = 1"));
        assertTrue(statement.sql.contains(
                "UNION ALL SELECT 'stg' AS detail_environment"));
        assertTrue(statement.sql.contains(
                "UNION ALL SELECT 'dev' AS detail_environment"));
        assertEquals("Acme", statement.parameters.get(1));
        assertEquals("Acme", statement.parameters.get(2));
        assertEquals("Acme", statement.parameters.get(3));
        assertTrue(!statement.sql.contains("SELECT *"));
    }

    @Test
    void insertUsesOneConnectionOneTransactionAndDevelopmentTable() {
        FakeJdbc jdbc = new FakeJdbc(false);
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);
        CustomerDetailDTO detail = detail();

        assertTrue(dao.saveOrUpdateCustomerDetailDev(detail));

        assertTransaction(jdbc, 1, 0);
        assertEquals(List.of(
                "UPDATE vertica_customer_detail_dev SET",
                insertPrefix("vertica_customer_detail_dev")),
                jdbc.sqlPrefixes());
        assertParameters(jdbc.statements.get(1).parameters, insertParameters(detail));
    }

    @Test
    void updateUsesOneConnectionOneTransactionAndStagingTable() {
        FakeJdbc jdbc = new FakeJdbc(true);
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);
        CustomerDetailDTO detail = detail();

        assertTrue(dao.saveOrUpdateCustomerDetailStg(detail));

        assertTransaction(jdbc, 1, 0);
        assertEquals(List.of(
                "UPDATE vertica_customer_detail_stg SET"),
                jdbc.sqlPrefixes());
        List<Object> expected = mutableParameters(detail);
        expected.add(detail.getCustomerName());
        assertParameters(jdbc.statements.getFirst().parameters, expected);
    }

    @Test
    void productionDetailWriteStoresStableAssigneeIdsInTheSameTransaction() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "user_id", "main-id", "user_count", 1));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "user_id", "sub-id", "user_count", 1));
        jdbc.enqueueUpdate(1);
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);

        assertTrue(dao.saveOrUpdateCustomerDetail(detail(), "actor-id"));

        assertEquals(3, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord update = jdbc.statements.get(2);
        assertTrue(update.sql.contains("main_manager_user_id = ?"));
        assertTrue(update.sql.contains("sub_manager_user_id = ?"));
        assertEquals("main-id", update.parameters.get(50));
        assertEquals("sub-id", update.parameters.get(51));
        assertEquals("customer-name", update.parameters.get(52));
        assertEquals(1, jdbc.commitCount);
        assertEquals(0, jdbc.rollbackCount);
        assertEquals(List.of(false, true), jdbc.autoCommitValues);
    }

    @Test
    void duplicateFirstInsertRetriesUpdateOnANewTransaction() {
        FakeJdbc jdbc = new FakeJdbc(false);
        jdbc.duplicateFirstInsert = true;
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);
        CustomerDetailDTO detail = detail();

        assertTrue(dao.saveOrUpdateCustomerDetailDev(detail));

        assertEquals(List.of(
                "UPDATE vertica_customer_detail_dev SET",
                insertPrefix("vertica_customer_detail_dev"),
                "UPDATE vertica_customer_detail_dev SET"),
                jdbc.sqlPrefixes());
        assertEquals(2, jdbc.openCount);
        assertEquals(1, jdbc.commitCount);
        assertEquals(1, jdbc.rollbackCount);
        assertEquals(2, jdbc.closeCount);
        assertEquals(List.of(false, true, false, true),
                jdbc.autoCommitValues);
    }

    @Test
    void sameCustomerWritesAreSerializedWithinTheApplication()
            throws Exception {
        AtomicInteger activeConnections = new AtomicInteger();
        AtomicInteger maximumActiveConnections = new AtomicInteger();
        CustomerDetailDAO dao = new CustomerDetailDAO(() ->
                serializedConnection(
                        activeConnections, maximumActiveConnections));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return dao.saveOrUpdateCustomerDetailDev(detail());
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return dao.saveOrUpdateCustomerDetailDev(detail());
            });
            assertTrue(ready.await(1, TimeUnit.SECONDS));
            start.countDown();

            assertTrue(first.get(2, TimeUnit.SECONDS));
            assertTrue(second.get(2, TimeUnit.SECONDS));
            assertEquals(1, maximumActiveConnections.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void readOnlyWriteFailureRollsBackAndKeepsExplicitClassification() {
        FakeJdbc jdbc = new FakeJdbc(false);
        jdbc.writeFailure = new SQLNonTransientException("blocked", "25006");
        CustomerDetailDAO dao = new CustomerDetailDAO(jdbc::open);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> dao.saveOrUpdateCustomerDetail(detail()));

        assertTrue(exception.isReadOnlyViolation());
        assertTransaction(jdbc, 0, 1);
    }

    @Test
    void daoCommitsAndRollsBackEvenWhenProviderStartsWithAutoCommitDisabled() {
        FakeJdbc successful = new FakeJdbc(false, false);
        CustomerDetailDAO successfulDao = new CustomerDetailDAO(successful::open);

        assertTrue(successfulDao.saveOrUpdateCustomerDetailDev(detail()));
        assertEquals(1, successful.commitCount);
        assertEquals(0, successful.rollbackCount);
        assertTrue(successful.autoCommitValues.isEmpty());
        assertEquals(1, successful.closeCount);

        FakeJdbc failing = new FakeJdbc(false, false);
        failing.writeFailure = new SQLNonTransientException("blocked", "25006");
        CustomerDetailDAO failingDao = new CustomerDetailDAO(failing::open);

        assertThrows(
                DataAccessException.class,
                () -> failingDao.saveOrUpdateCustomerDetailDev(detail()));
        assertEquals(0, failing.commitCount);
        assertEquals(1, failing.rollbackCount);
        assertTrue(failing.autoCommitValues.isEmpty());
        assertEquals(1, failing.closeCount);
    }

    private static void assertTransaction(FakeJdbc jdbc, int commits, int rollbacks) {
        assertEquals(1, jdbc.openCount);
        assertEquals(List.of(false, true), jdbc.autoCommitValues);
        assertEquals(commits, jdbc.commitCount);
        assertEquals(rollbacks, jdbc.rollbackCount);
        assertEquals(1, jdbc.closeCount);
    }

    private static Connection serializedConnection(
            AtomicInteger activeConnections,
            AtomicInteger maximumActiveConnections) throws SQLException {
        int active = activeConnections.incrementAndGet();
        maximumActiveConnections.accumulateAndGet(active, Math::max);
        try {
            Thread.sleep(75);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            activeConnections.decrementAndGet();
            throw new SQLException("Interrupted while opening test connection",
                    exception);
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "getAutoCommit" -> true;
                    case "setAutoCommit", "commit", "rollback" -> null;
                    case "prepareStatement" -> serializedStatement();
                    case "close" -> {
                        activeConnections.decrementAndGet();
                        yield null;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static PreparedStatement serializedStatement() {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "setString", "setTimestamp", "close" -> null;
                    case "executeUpdate" -> 1;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static String insertPrefix(String tableName) {
        return "INSERT INTO " + tableName + " (";
    }

    private static void assertParameters(Map<Integer, Object> actual, List<Object> expected) {
        assertEquals(50, actual.size());
        assertEquals(50, expected.size());
        for (int index = 1; index <= expected.size(); index++) {
            assertEquals(expected.get(index - 1), actual.get(index), "parameter " + index);
        }
    }

    private static List<Object> insertParameters(CustomerDetailDTO detail) {
        List<Object> parameters = new ArrayList<>();
        parameters.add(detail.getCustomerName());
        parameters.addAll(mutableParameters(detail));
        return parameters;
    }

    private static List<Object> mutableParameters(CustomerDetailDTO detail) {
        return new ArrayList<>(List.of(
                detail.getSystemName(),
                detail.getCustomerManager(),
                detail.getSiCompany(),
                detail.getSiManager(),
                detail.getCreator(),
                new Timestamp(detail.getCreateDate().getTime()),
                detail.getMainManager(),
                detail.getSubManager(),
                new Timestamp(detail.getInstallDate().getTime()),
                detail.getIntroductionYear(),
                detail.getDbName(),
                detail.getDbMode(),
                detail.getVerticaVersion(),
                detail.getLicenseInfo(),
                detail.getSaid(),
                detail.getNodeCount(),
                detail.getVerticaAdmin(),
                detail.getSubclusterYn(),
                detail.getMcYn(),
                detail.getMcHost(),
                detail.getMcVersion(),
                detail.getMcAdmin(),
                detail.getBackupYn(),
                detail.getCustomResourcePoolYn(),
                detail.getBackupNote(),
                detail.getOsInfo(),
                detail.getMemoryInfo(),
                detail.getSwapMemory(),
                detail.getInfraType(),
                detail.getCpuSocket(),
                detail.getHyperThreading(),
                detail.getCpuCore(),
                detail.getDataArea(),
                detail.getDepotArea(),
                detail.getCatalogArea(),
                detail.getObjectArea(),
                detail.getPublicYn(),
                detail.getPublicNetwork(),
                detail.getPrivateYn(),
                detail.getPrivateNetwork(),
                detail.getStorageYn(),
                detail.getStorageNetwork(),
                detail.getEtlTool(),
                detail.getBiTool(),
                detail.getDbEncryption(),
                detail.getCdcTool(),
                new Timestamp(detail.getEosDate().getTime()),
                detail.getCustomerType(),
                detail.getNote()));
    }

    private static CustomerDetailDTO detail() {
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("customer-name");
        detail.setSystemName("system-name");
        detail.setCustomerManager("customer-manager");
        detail.setSiCompany("si-company");
        detail.setSiManager("si-manager");
        detail.setCreator("creator");
        detail.setCreateDate(new java.util.Date(1_000L));
        detail.setMainManager("main-manager");
        detail.setSubManager("sub-manager");
        detail.setInstallDate(new java.util.Date(2_000L));
        detail.setIntroductionYear("introduction-year");
        detail.setDbName("db-name");
        detail.setDbMode("db-mode");
        detail.setVerticaVersion("vertica-version");
        detail.setLicenseInfo("license-info");
        detail.setSaid("said");
        detail.setNodeCount("node-count");
        detail.setVerticaAdmin("vertica-admin");
        detail.setSubclusterYn("subcluster-yn");
        detail.setMcYn("mc-yn");
        detail.setMcHost("mc-host");
        detail.setMcVersion("mc-version");
        detail.setMcAdmin("mc-admin");
        detail.setBackupYn("backup-yn");
        detail.setCustomResourcePoolYn("resource-pool-yn");
        detail.setBackupNote("backup-note");
        detail.setOsInfo("os-info");
        detail.setMemoryInfo("memory-info");
        detail.setSwapMemory("swap-memory");
        detail.setInfraType("infra-type");
        detail.setCpuSocket("cpu-socket");
        detail.setHyperThreading("hyper-threading");
        detail.setCpuCore("cpu-core");
        detail.setDataArea("data-area");
        detail.setDepotArea("depot-area");
        detail.setCatalogArea("catalog-area");
        detail.setObjectArea("object-area");
        detail.setPublicYn("public-yn");
        detail.setPublicNetwork("public-network");
        detail.setPrivateYn("private-yn");
        detail.setPrivateNetwork("private-network");
        detail.setStorageYn("storage-yn");
        detail.setStorageNetwork("storage-network");
        detail.setEtlTool("etl-tool");
        detail.setBiTool("bi-tool");
        detail.setDbEncryption("db-encryption");
        detail.setCdcTool("cdc-tool");
        detail.setEosDate(new java.util.Date(3_000L));
        detail.setCustomerType("customer-type");
        detail.setNote("note");
        return detail;
    }

    private static final class FakeJdbc {
        private final boolean existing;
        private final List<StatementRecord> statements = new ArrayList<>();
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private int openCount;
        private int commitCount;
        private int rollbackCount;
        private int closeCount;
        private SQLException writeFailure;
        private boolean duplicateFirstInsert;
        private boolean concurrentRowCreated;
        private boolean autoCommit = true;

        private FakeJdbc(boolean existing) {
            this(existing, true);
        }

        private FakeJdbc(boolean existing, boolean initialAutoCommit) {
            this.existing = existing;
            this.autoCommit = initialAutoCommit;
        }

        private Connection open() {
            openCount++;
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "prepareStatement" -> statement((String) args[0]);
                        case "getMetaData" -> metadata();
                        case "getAutoCommit" -> autoCommit;
                        case "setAutoCommit" -> {
                            autoCommit = (Boolean) args[0];
                            autoCommitValues.add(autoCommit);
                            yield null;
                        }
                        case "commit" -> {
                            commitCount++;
                            yield null;
                        }
                        case "rollback" -> {
                            rollbackCount++;
                            yield null;
                        }
                        case "close" -> {
                            closeCount++;
                            yield null;
                        }
                        case "isClosed" -> false;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private static DatabaseMetaData metadata() {
            return (DatabaseMetaData) Proxy.newProxyInstance(
                    DatabaseMetaData.class.getClassLoader(),
                    new Class<?>[] {DatabaseMetaData.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getColumns" -> resultSet(false);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private PreparedStatement statement(String sql) {
            StatementRecord record = new StatementRecord(sql);
            statements.add(record);
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "setString", "setTimestamp" -> {
                            record.parameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> resultSet(existing);
                        case "executeUpdate" -> {
                            if (writeFailure != null) {
                                throw writeFailure;
                            }
                            if (record.sql.startsWith("UPDATE")) {
                                yield existing || concurrentRowCreated ? 1 : 0;
                            }
                            if (record.sql.startsWith("INSERT")
                                    && duplicateFirstInsert) {
                                duplicateFirstInsert = false;
                                concurrentRowCreated = true;
                                throw new SQLException(
                                        "duplicate customer", "23505");
                            }
                            yield 1;
                        }
                        case "close" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private List<String> sqlPrefixes() {
            return statements.stream()
                    .map(record -> {
                        if (record.sql.startsWith("INSERT INTO")) {
                            return record.sql.substring(0, record.sql.indexOf('(') + 1);
                        }
                        if (record.sql.startsWith("UPDATE")) {
                            return record.sql.substring(0, record.sql.indexOf(" SET") + 4);
                        }
                        return record.sql;
                    })
                    .toList();
        }

        private static ResultSet resultSet(boolean firstResult) {
            boolean[] first = {true};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[] {ResultSet.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "next" -> {
                            boolean result = first[0] && firstResult;
                            first[0] = false;
                            yield result;
                        }
                        case "close" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class StatementRecord {
        private final String sql;
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();

        private StatementRecord(String sql) {
            this.sql = sql;
        }
    }

}
