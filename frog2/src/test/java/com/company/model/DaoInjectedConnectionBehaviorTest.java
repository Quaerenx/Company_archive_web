package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DaoInjectedConnectionBehaviorTest {
    @Test
    void maintenancePointAndMonthLookupsUseInjectedConnections() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(maintenanceRow(17L, "2026-08-12"));
        jdbc.enqueue(maintenanceRow(18L, "2026-08-20"));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        MaintenanceRecordDTO record = dao.getMaintenanceRecordById(17L);
        List<MaintenanceRecordDTO> month =
                dao.getMaintenanceRecordsByMonth(
                        Date.valueOf("2026-08-01"),
                        Date.valueOf("2026-09-01"));

        assertEquals(17L, record.getMaintenanceId());
        assertEquals(18L, month.getFirst().getMaintenanceId());
        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE maintenance_id = ?"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "inspection_date >= ? AND inspection_date < ?"));
    }

    @Test
    void customerListAndPointLookupUseInjectedConnections() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(customerRow("Acme"));
        jdbc.enqueue(customerRow("Acme"));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        List<CustomerDTO> customers = dao.getMaintenanceCustomers(
                "manager_name", "DESC");
        CustomerDTO customer = dao.getCustomerByName("Acme");

        assertEquals("Acme", customers.getFirst().getCustomerName());
        assertEquals("23.4", customer.getVerticaVersion());
        assertEquals("2021", customer.getFirstIntroductionYear());
        assertEquals("2028-10-31", customer.getVerticaEos());
        assertEquals("storage-network", customer.getOsStorageConfig());
        assertEquals("backup-note", customer.getBackupConfig());
        assertEquals("etl", customer.getEtlTool());
        assertEquals("bi", customer.getBiTool());
        assertEquals("encryption", customer.getDbEncryption());
        assertEquals("cdc", customer.getCdcTool());
        assertEquals("customer-note", customer.getNote());
        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
        assertTrue(jdbc.statements.get(0).sql.contains(
                "d.customer_type = '정기점검 계약 고객사'"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "NULLIF(TRIM(d.main_manager), '') IS NULL"));
        assertTrue(jdbc.statements.get(0).sql.endsWith(
                "d.main_manager DESC, d.customer_name ASC"));
        assertEquals("Acme", jdbc.statements.get(1).parameters.get(1));
    }

    @Test
    void assignedMaintenanceCustomersAreFilteredInTheDatabase() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(customerRow("Acme"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        List<CustomerDTO> customers =
                dao.getMaintenanceCustomersByAssignee(
                        "alice-id", " Alice ");

        assertEquals(List.of("Acme"), customers.stream()
                .map(CustomerDTO::getCustomerName)
                .toList());
        assertEquals(1, jdbc.openCount);
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("d.customer_type = ?"));
        assertTrue(statement.sql.contains(
                "LOWER(TRIM(d.main_manager)) = LOWER(?) "
                        + "OR LOWER(TRIM(d.sub_manager)) = LOWER(?)"));
        assertEquals("정기점검 계약 고객사", statement.parameters.get(1));
        assertEquals("Alice", statement.parameters.get(2));
        assertEquals("Alice", statement.parameters.get(3));
    }

    @Test
    void assignedMaintenanceCustomersUseStableIdsAfterMigration() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id");
        jdbc.enqueue(customerRow("Acme"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        List<CustomerDTO> customers =
                dao.getMaintenanceCustomersByAssignee(
                        "alice-id", "Old Alice Name");

        assertEquals(List.of("Acme"), customers.stream()
                .map(CustomerDTO::getCustomerName)
                .toList());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "d.main_manager_user_id = ? "
                        + "OR d.sub_manager_user_id = ?"));
        assertEquals("alice-id", statement.parameters.get(2));
        assertEquals("alice-id", statement.parameters.get(3));
        assertFalse(statement.sql.contains("LOWER(TRIM(d.main_manager))"));
    }

    @Test
    void blankAssigneeDoesNotOpenACustomerConnection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertTrue(dao.getMaintenanceCustomersByAssignee(null, " ").isEmpty());
        assertEquals(0, jdbc.openCount);
    }

    @Test
    void activeMaintenanceCustomerValidationUsesTheCustomerType() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(customerRow("Maintenance", "정기점검 계약 고객사"));
        jdbc.enqueue(customerRow("General", "일반 고객사"));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        assertTrue(dao.isActiveMaintenanceCustomer("Maintenance"));
        assertFalse(dao.isActiveMaintenanceCustomer("General"));

        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
    }

    @Test
    void customerMutationsUseInjectedConnectionsAndCloseThem() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);
        CustomerDTO customer = customer("Acme");

        assertTrue(dao.updateCustomer(customer));
        assertTrue(dao.addCustomer(customer));
        assertTrue(dao.deleteCustomer("Acme"));

        assertEquals(3, jdbc.openCount);
        assertEquals(3, jdbc.closeCount);
        assertTrue(jdbc.statements.get(0).sql.startsWith(
                "UPDATE vertica_customer_detail SET"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "introduction_year = ?, eos_date = ?, storage_network = ?, backup_note = ?"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "etl_tool = ?, bi_tool = ?, db_encryption = ?, cdc_tool = ?, note = ?"));
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "INSERT INTO vertica_customer_detail"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "introduction_year, eos_date, storage_network, backup_note, etl_tool, bi_tool, db_encryption, cdc_tool, note"));
        assertTrue(jdbc.statements.get(2).sql.contains(
                "SET is_deleted = 0"));
        assertEquals("archive", jdbc.statements.get(0).parameters.get(1));
        assertEquals("정기점검 계약 고객사",
                jdbc.statements.get(0).parameters.get(10));
        assertEquals("2021", jdbc.statements.get(0).parameters.get(11));
        assertEquals(Date.valueOf("2028-10-31"),
                jdbc.statements.get(0).parameters.get(12));
        assertEquals("storage-network",
                jdbc.statements.get(0).parameters.get(13));
        assertEquals("backup-note",
                jdbc.statements.get(0).parameters.get(14));
        assertEquals("etl", jdbc.statements.get(0).parameters.get(15));
        assertEquals("bi", jdbc.statements.get(0).parameters.get(16));
        assertEquals("encryption", jdbc.statements.get(0).parameters.get(17));
        assertEquals("cdc", jdbc.statements.get(0).parameters.get(18));
        assertEquals("customer-note",
                jdbc.statements.get(0).parameters.get(19));
        assertEquals("Acme", jdbc.statements.get(0).parameters.get(20));
        assertEquals("Acme", jdbc.statements.get(1).parameters.get(1));
        assertEquals("archive", jdbc.statements.get(1).parameters.get(2));
        assertEquals("정기점검 계약 고객사",
                jdbc.statements.get(1).parameters.get(11));
        assertEquals("2021", jdbc.statements.get(1).parameters.get(12));
        assertEquals(Date.valueOf("2028-10-31"),
                jdbc.statements.get(1).parameters.get(13));
        assertEquals("customer-note",
                jdbc.statements.get(1).parameters.get(20));
    }

    @Test
    void customerUpdatePersistsResolvedStableAssigneeIds() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "user_id", "alice-id", "user_count", 1));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "user_id", "bob-id", "user_count", 1));
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        assertTrue(dao.updateCustomer(customer("Acme")));

        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "LOWER(TRIM(userName)) = LOWER(?)"));
        assertEquals("Alice", jdbc.statements.get(0).parameters.get(1));
        assertEquals("Bob", jdbc.statements.get(1).parameters.get(1));
        PaginationJdbcFixture.StatementRecord update = jdbc.statements.get(2);
        assertTrue(update.sql.contains("main_manager_user_id = ?"));
        assertTrue(update.sql.contains("sub_manager_user_id = ?"));
        assertEquals("alice-id", update.parameters.get(20));
        assertEquals("bob-id", update.parameters.get(21));
        assertEquals("Acme", update.parameters.get(22));
    }

    @Test
    void meetingRecordReadsAndCreateUseInjectedConnections() {
        Timestamp meetingTime = Timestamp.valueOf("2026-08-22 09:30:00");
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(meetingRow(17L, meetingTime));
        jdbc.enqueue(PaginationJdbcFixture.row("count", 1));
        jdbc.enqueue(meetingDetailRow(17L, meetingTime));
        jdbc.enqueueUpdate(1);
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        List<MeetingRecordDTO> records = dao.getMeetingRecords(1);
        int count = dao.getTotalCount();
        MeetingRecordDTO detail = dao.getMeetingRecord(17L);
        assertTrue(dao.addMeetingRecord(meetingRecord(meetingTime)));

        assertEquals(17L, records.getFirst().getMeetingId());
        assertEquals(1, count);
        assertEquals("Meeting detail", detail.getContent());
        assertEquals(4, jdbc.openCount);
        assertEquals(4, jdbc.closeCount);
        assertEquals(MeetingRecordDAO.getPageSize(),
                jdbc.statements.get(0).parameters.get(1));
        assertEquals(0, jdbc.statements.get(0).parameters.get(2));
        assertEquals(17L, jdbc.statements.get(2).parameters.get(1));
        assertTrue(jdbc.statements.get(3).sql.startsWith(
                "INSERT INTO meeting_records"));
    }

    @Test
    void meetingCommentCreateUsesInjectedConnection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        MeetingCommentDAO dao = new MeetingCommentDAO(jdbc::open);
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setMeetingId(17L);
        comment.setContent("Follow-up");
        comment.setAuthorId("user-17");
        comment.setAuthorName("Alice");

        assertTrue(dao.addComment(comment));

        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(17L, jdbc.statements.getFirst().parameters.get(1));
        assertEquals("Follow-up", jdbc.statements.getFirst().parameters.get(2));
    }

    @Test
    void userVmHostReadsAndDeleteUseInjectedConnections() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(userVmHostRow("192.168.40.17"));
        jdbc.enqueue(PaginationJdbcFixture.row("count", 1));
        jdbc.enqueue(userVmHostRow("192.168.40.17"));
        jdbc.enqueueUpdate(1);
        UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);

        List<UserVmHostDTO> hosts = dao.getActiveHostsByOwner("user-17");
        int count = dao.getActiveHostCountByOwner("user-17");
        UserVmHostDTO host = dao.getHostByIpAndOwner(
                "192.168.40.17", "user-17");
        boolean deleted = dao.deleteByIpAndOwner(
                "192.168.40.17", "user-17");

        assertEquals("192.168.40.17", hosts.getFirst().getIp());
        assertEquals(1, count);
        assertEquals("Test host", host.getPurpose());
        assertTrue(deleted);
        assertEquals(4, jdbc.openCount);
        assertEquals(4, jdbc.closeCount);
    }

    @Test
    void userVmHostDeleteRequiresExactlyOneAffectedRow() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(2);
        UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);

        assertFalse(dao.deleteByIpAndOwner(
                "192.168.40.17", "user-17"));
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void userVmHostSaveKeepsChecksAndInsertOnOneConnection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        jdbc.enqueueUpdate(1);
        UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);
        UserVmHostDTO host = userVmHost("192.168.40.18");

        assertEquals(
                UserVmHostDAO.MutationResult.SAVED,
                dao.saveNormalized(host, null));

        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains("WHERE ip = ?"));
        assertTrue(jdbc.statements.get(1).sql.startsWith("SELECT COUNT(*)"));
        assertTrue(jdbc.statements.get(2).sql.startsWith(
                "INSERT INTO user_vm_hosts"));
    }

    @Test
    void userVmHostWritesReportZeroAffectedRows() {
        PaginationJdbcFixture insertJdbc = new PaginationJdbcFixture();
        insertJdbc.enqueue();
        insertJdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        insertJdbc.enqueueUpdate(0);
        UserVmHostDAO insertDao = new UserVmHostDAO(insertJdbc::open);
        UserVmHostDTO host = userVmHost("192.168.40.18");

        assertEquals(
                UserVmHostDAO.MutationResult.WRITE_FAILED,
                insertDao.saveNormalized(host, null));

        PaginationJdbcFixture updateJdbc = new PaginationJdbcFixture();
        updateJdbc.enqueue(userVmHostRow("192.168.40.17"));
        updateJdbc.enqueue(userVmHostRow("192.168.40.17"));
        updateJdbc.enqueueUpdate(0);
        UserVmHostDAO updateDao = new UserVmHostDAO(updateJdbc::open);
        UserVmHostDTO updatedHost = userVmHost("192.168.40.17");

        assertEquals(
                UserVmHostDAO.MutationResult.WRITE_FAILED,
                updateDao.saveNormalized(
                        updatedHost, "192.168.40.17"));
        assertEquals(1, updateJdbc.openCount);
    }

    @SuppressWarnings("deprecation")
    @Test
    void userVmHostLegacySaveAdapterKeepsTheOriginalContract() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        jdbc.enqueueUpdate(1);
        UserVmHostDAO dao = new UserVmHostDAO(jdbc::open);

        assertNull(dao.save(userVmHost("192.168.40.18"), null));
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);

        assertEquals(
                "IP는 192.168.40.1 ~ 192.168.40.254 범위만 등록할 수 있습니다.",
                dao.save(userVmHost("10.0.0.1"), null));
        assertEquals(1, jdbc.openCount);
    }

    private static java.util.Map<String, Object> maintenanceRow(
            long id, String date) {
        return PaginationJdbcFixture.row(
                "maintenance_id", id,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf(date),
                "vertica_version", "23.4",
                "note", null,
                "created_at", null,
                "updated_at", null);
    }

    private static java.util.Map<String, Object> customerRow(String name) {
        return customerRow(name, "정기점검 계약 고객사");
    }

    private static java.util.Map<String, Object> customerRow(
            String name, String customerType) {
        return PaginationJdbcFixture.row(
                "customer_name", name,
                "vertica_version", "23.4",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "25TB",
                "said", "SAID",
                "main_manager", "Alice",
                "sub_manager", "Bob",
                "db_name", "archive",
                "customer_type", customerType,
                "introduction_year", "2021",
                "eos_date", Date.valueOf("2028-10-31"),
                "storage_network", "storage-network",
                "backup_note", "backup-note",
                "etl_tool", "etl",
                "bi_tool", "bi",
                "db_encryption", "encryption",
                "cdc_tool", "cdc",
                "note", "customer-note");
    }

    private static CustomerDTO customer(String name) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        customer.setDbName("archive");
        customer.setVerticaVersion("23.4");
        customer.setMode("ENT");
        customer.setOs("Linux");
        customer.setNodes("3");
        customer.setLicenseSize("25TB");
        customer.setManagerName("Alice");
        customer.setSubManagerName("Bob");
        customer.setSaid("SAID");
        customer.setCustomerType("정기점검 계약 고객사");
        customer.setFirstIntroductionYear("2021");
        customer.setVerticaEos("2028-10-31");
        customer.setOsStorageConfig("storage-network");
        customer.setBackupConfig("backup-note");
        customer.setEtlTool("etl");
        customer.setBiTool("bi");
        customer.setDbEncryption("encryption");
        customer.setCdcTool("cdc");
        customer.setNote("customer-note");
        return customer;
    }

    private static java.util.Map<String, Object> meetingRow(
            long id, Timestamp meetingTime) {
        return PaginationJdbcFixture.row(
                "meeting_id", id,
                "title", "Weekly meeting",
                "meeting_type", "weekly",
                "author_name", "Alice",
                "meeting_datetime", meetingTime);
    }

    private static java.util.Map<String, Object> meetingDetailRow(
            long id, Timestamp meetingTime) {
        return PaginationJdbcFixture.row(
                "meeting_id", id,
                "title", "Weekly meeting",
                "meeting_datetime", meetingTime,
                "meeting_type", "weekly",
                "content", "Meeting detail",
                "author_id", "user-17",
                "author_name", "Alice",
                "view_count", 0,
                "created_at", meetingTime,
                "updated_at", meetingTime);
    }

    private static MeetingRecordDTO meetingRecord(Timestamp meetingTime) {
        MeetingRecordDTO record = new MeetingRecordDTO();
        record.setTitle("Weekly meeting");
        record.setMeetingDatetime(meetingTime);
        record.setMeetingType("weekly");
        record.setContent("Meeting detail");
        record.setAuthorId("user-17");
        record.setAuthorName("Alice");
        return record;
    }

    private static java.util.Map<String, Object> userVmHostRow(String ip) {
        Timestamp now = Timestamp.valueOf("2026-08-22 10:00:00");
        return PaginationJdbcFixture.row(
                "ip", ip,
                "owner_user_id", "user-17",
                "owner_user_name", "Alice",
                "purpose", "Test host",
                "os_info", "Linux",
                "vertica_version", "23.4",
                "remote_host", "node-a",
                "note", null,
                "status", "ACTIVE",
                "created_at", now,
                "updated_at", now);
    }

    private static UserVmHostDTO userVmHost(String ip) {
        UserVmHostDTO host = new UserVmHostDTO();
        host.setIp(ip);
        host.setOwnerUserId("user-17");
        host.setOwnerUserName("Alice");
        host.setPurpose("Test host");
        host.setOsInfo("Linux");
        host.setVerticaVersion("23.4");
        host.setRemoteHost("node-a");
        return host;
    }
}
