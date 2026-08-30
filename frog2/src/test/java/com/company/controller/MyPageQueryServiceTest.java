package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import com.company.mypage.WorkInboxService;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyPageQueryServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-30T01:00:00Z"), ZoneOffset.UTC);

    @Test
    void overviewLoadsOnlySummaryQueries() {
        UserDTO user = user("user-1");
        MaintenanceRecordDTO maintenance = new MaintenanceRecordDTO();
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        RecordingUserVmHostDAO hostDAO = new RecordingUserVmHostDAO();
        RecordingCustomerDAO customerDAO = new RecordingCustomerDAO();
        MyPageQueryService service = new MyPageQueryService(
                new StubUserDAO(user),
                customerDAO,
                new StubMaintenanceDAO(maintenance),
                new StubTroubleshootingDAO(troubleshooting),
                hostDAO,
                new WorkInboxService(),
                FIXED_CLOCK);

        MyPageQueryService.ViewData result =
                service.loadOverview("user-1", 5);

        assertSame(user, result.user());
        assertEquals(List.of(maintenance), result.recentMaintenanceRecords());
        assertEquals(12, result.maintenanceCount());
        assertEquals(List.of(troubleshooting), result.recentTroubleshootings());
        assertEquals(7, result.troubleshootingCount());
        assertEquals(3, result.vmHostCount());
        assertEquals(20, result.vmHostLimit());
        assertEquals(0, result.workInbox().getTotalCount());
        assertEquals(0, hostDAO.listCalls);
        assertEquals(1, hostDAO.countCalls);
        assertEquals(1, customerDAO.listCalls);
        assertEquals(0, customerDAO.assignmentCalls);
    }

    @Test
    void overviewBuildsInboxFromAssignedCustomersWithBatchQueries() {
        UserDTO user = user("user-3");
        CustomerDTO customer = completeCustomer("Alpha", "Tester");
        MaintenanceRecordDTO latest = new MaintenanceRecordDTO();
        latest.setCustomerName("Alpha");
        latest.setInspectionDate(Date.valueOf("2026-07-20"));
        latest.setLicenseUsagePct("106.0");
        RecordingCustomerDAO customerDAO = new RecordingCustomerDAO(
                List.of(customer),
                List.of(new MaintenanceCustomerAssignment(
                        "Alpha", "Tester")));
        RecordingMaintenanceDAO maintenanceDAO =
                new RecordingMaintenanceDAO(List.of(), List.of(latest));
        MyPageQueryService service = new MyPageQueryService(
                new StubUserDAO(user),
                customerDAO,
                maintenanceDAO,
                new RecordingTroubleshootingDAO(),
                new RecordingUserVmHostDAO(),
                new WorkInboxService(),
                FIXED_CLOCK);

        MyPageQueryService.ViewData result =
                service.loadOverview("user-3", 5);

        assertEquals(2, result.workInbox().getTotalCount());
        assertEquals(1, result.workInbox().getDangerCount());
        assertEquals(1, result.workInbox().getWarningCount());
        assertEquals(1, customerDAO.listCalls);
        assertEquals(1, customerDAO.assignmentCalls);
        assertEquals(1, maintenanceDAO.monthCalls);
        assertEquals(1, maintenanceDAO.latestCalls);
        assertEquals(List.of("Alpha"), maintenanceDAO.latestCustomerNames);
    }

    @Test
    void hostSectionLoadsHostListWithoutActivityQueries() {
        UserDTO user = user("user-2");
        UserVmHostDTO first = new UserVmHostDTO();
        UserVmHostDTO second = new UserVmHostDTO();
        RecordingUserVmHostDAO hostDAO =
                new RecordingUserVmHostDAO(List.of(first, second));
        RecordingMaintenanceDAO maintenanceDAO =
                new RecordingMaintenanceDAO();
        RecordingTroubleshootingDAO troubleshootingDAO =
                new RecordingTroubleshootingDAO();
        RecordingCustomerDAO customerDAO = new RecordingCustomerDAO();
        MyPageQueryService service = new MyPageQueryService(
                new StubUserDAO(user),
                customerDAO,
                maintenanceDAO,
                troubleshootingDAO,
                hostDAO,
                new WorkInboxService(),
                FIXED_CLOCK);

        MyPageQueryService.ViewData result = service.loadHosts("user-2");

        assertSame(user, result.user());
        assertEquals(List.of(first, second), result.vmHosts());
        assertEquals(2, result.vmHostCount());
        assertEquals(1, hostDAO.listCalls);
        assertEquals(0, hostDAO.countCalls);
        assertEquals(0, maintenanceDAO.calls);
        assertEquals(0, troubleshootingDAO.calls);
        assertEquals(0, customerDAO.listCalls);
        assertEquals(0, customerDAO.assignmentCalls);
    }

    private static UserDTO user(String userId) {
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        user.setUserName("Tester");
        return user;
    }

    private static CustomerDTO completeCustomer(
            String customerName, String managerName) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(customerName);
        customer.setManagerName(managerName);
        customer.setVerticaVersion("23.4.0-13");
        customer.setDbName("archive");
        customer.setNodes("3");
        customer.setLicenseSize("10TB");
        customer.setSaid("A-S100000000");
        return customer;
    }

    private static final class StubUserDAO extends UserDAO {
        private final UserDTO user;

        private StubUserDAO(UserDTO user) {
            this.user = user;
        }

        @Override
        public UserDTO getUserById(String userId) {
            return user;
        }
    }

    private static class RecordingMaintenanceDAO extends MaintenanceRecordDAO {
        private int calls;
        private int monthCalls;
        private int latestCalls;
        private List<String> latestCustomerNames = List.of();
        private final List<MaintenanceRecordDTO> monthRecords;
        private final List<MaintenanceRecordDTO> latestRecords;

        private RecordingMaintenanceDAO() {
            this(List.of(), List.of());
        }

        private RecordingMaintenanceDAO(
                List<MaintenanceRecordDTO> monthRecords,
                List<MaintenanceRecordDTO> latestRecords) {
            this.monthRecords = monthRecords;
            this.latestRecords = latestRecords;
        }

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByOwner(
                String userId, int page, int pageSize) {
            calls++;
            return new PageResult<>(List.of(), 0, 1, pageSize);
        }

        @Override
        public List<MaintenanceRecordDTO> getMaintenanceRecordsByMonth(
                Date startDate, Date endDate) {
            monthCalls++;
            return monthRecords;
        }

        @Override
        public List<MaintenanceRecordDTO>
                getLatestMaintenanceRecordsByCustomers(
                        List<String> customerNames) {
            latestCalls++;
            latestCustomerNames = List.copyOf(customerNames);
            return latestRecords;
        }
    }

    private static final class RecordingCustomerDAO extends CustomerDAO {
        private final List<CustomerDTO> customers;
        private final List<MaintenanceCustomerAssignment> assignments;
        private int listCalls;
        private int assignmentCalls;

        private RecordingCustomerDAO() {
            this(List.of(), List.of());
        }

        private RecordingCustomerDAO(
                List<CustomerDTO> customers,
                List<MaintenanceCustomerAssignment> assignments) {
            this.customers = customers;
            this.assignments = assignments;
        }

        @Override
        public List<CustomerDTO> getMaintenanceCustomers(
                String sortField, String sortDirection) {
            listCalls++;
            return customers;
        }

        @Override
        public List<MaintenanceCustomerAssignment>
                getAllMaintenanceCustomerAssignments() {
            assignmentCalls++;
            return assignments;
        }
    }

    private static final class StubMaintenanceDAO
            extends RecordingMaintenanceDAO {
        private final MaintenanceRecordDTO record;

        private StubMaintenanceDAO(MaintenanceRecordDTO record) {
            this.record = record;
        }

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByOwner(
                String userId, int page, int pageSize) {
            return new PageResult<>(List.of(record), 12, 1, pageSize);
        }
    }

    private static class RecordingTroubleshootingDAO
            extends TroubleshootingDAO {
        private int calls;

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPageByOwner(
                String userId, int page, int pageSize) {
            calls++;
            return new PageResult<>(List.of(), 0, 1, pageSize);
        }
    }

    private static final class StubTroubleshootingDAO
            extends RecordingTroubleshootingDAO {
        private final TroubleshootingDTO record;

        private StubTroubleshootingDAO(TroubleshootingDTO record) {
            this.record = record;
        }

        @Override
        public PageResult<TroubleshootingDTO> getTroubleshootingPageByOwner(
                String userId, int page, int pageSize) {
            return new PageResult<>(List.of(record), 7, 1, pageSize);
        }
    }

    private static final class RecordingUserVmHostDAO
            extends UserVmHostDAO {
        private final List<UserVmHostDTO> hosts;
        private int listCalls;
        private int countCalls;

        private RecordingUserVmHostDAO() {
            this(List.of());
        }

        private RecordingUserVmHostDAO(List<UserVmHostDTO> hosts) {
            this.hosts = hosts;
        }

        @Override
        public int getMaxHostsPerUser() {
            return 20;
        }

        @Override
        public List<UserVmHostDTO> getActiveHostsByOwner(String userId) {
            listCalls++;
            return hosts;
        }

        @Override
        public int getActiveHostCountByOwner(String userId) {
            countCalls++;
            return 3;
        }
    }
}
