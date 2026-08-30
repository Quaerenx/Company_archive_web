package com.company.controller;

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
import com.company.mypage.WorkInbox;
import com.company.mypage.WorkInboxService;
import com.company.util.BusinessDate;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

final class MyPageQueryService {
    private static final int WORK_INBOX_DISPLAY_LIMIT = 8;

    private final UserDAO userDAO;
    private final CustomerDAO customerDAO;
    private final MaintenanceRecordDAO maintenanceDAO;
    private final TroubleshootingDAO troubleshootingDAO;
    private final UserVmHostDAO userVmHostDAO;
    private final WorkInboxService workInboxService;
    private final Clock clock;

    MyPageQueryService() {
        this(
                new UserDAO(),
                new CustomerDAO(),
                new MaintenanceRecordDAO(),
                new TroubleshootingDAO(),
                new UserVmHostDAO(),
                new WorkInboxService(),
                BusinessDate.systemClock());
    }

    MyPageQueryService(
            UserDAO userDAO,
            CustomerDAO customerDAO,
            MaintenanceRecordDAO maintenanceDAO,
            TroubleshootingDAO troubleshootingDAO,
            UserVmHostDAO userVmHostDAO,
            WorkInboxService workInboxService,
            Clock clock) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
        this.userVmHostDAO = Objects.requireNonNull(
                userVmHostDAO, "userVmHostDAO");
        this.workInboxService = Objects.requireNonNull(
                workInboxService, "workInboxService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    ViewData loadOverview(String userId, int recentActivityLimit) {
        UserDTO user = userDAO.getUserById(userId);
        PageResult<MaintenanceRecordDTO> maintenance =
                maintenanceDAO.getMaintenanceRecordsByOwner(
                        userId, 1, recentActivityLimit);
        PageResult<TroubleshootingDTO> troubleshooting =
                troubleshootingDAO.getTroubleshootingPageByOwner(
                        userId, 1, recentActivityLimit);
        int hostLimit = userVmHostDAO.getMaxHostsPerUser();
        int hostCount = userVmHostDAO.getActiveHostCountByOwner(userId);
        WorkInbox workInbox = loadWorkInbox(user);
        return new ViewData(
                user,
                maintenance.items(),
                maintenance.totalCount(),
                troubleshooting.items(),
                troubleshooting.totalCount(),
                workInbox,
                List.of(),
                hostLimit,
                hostCount);
    }

    ViewData loadHosts(String userId) {
        UserDTO user = userDAO.getUserById(userId);
        int hostLimit = userVmHostDAO.getMaxHostsPerUser();
        List<UserVmHostDTO> hosts =
                userVmHostDAO.getActiveHostsByOwner(userId);
        return new ViewData(
                user,
                List.of(),
                0,
                List.of(),
                0,
                WorkInbox.empty(),
                hosts,
                hostLimit,
                hosts.size());
    }

    private WorkInbox loadWorkInbox(UserDTO user) {
        if (user == null || user.getUserName() == null) {
            return WorkInbox.empty();
        }
        List<CustomerDTO> maintenanceCustomers =
                customerDAO.getMaintenanceCustomers(
                        "manager_name", "ASC");
        List<CustomerDTO> assignedCustomers =
                workInboxService.assignedCustomers(
                        user.getUserName(), maintenanceCustomers);
        if (assignedCustomers.isEmpty()) {
            return WorkInbox.empty();
        }

        LocalDate today = BusinessDate.today(clock);
        YearMonth currentMonth = YearMonth.from(today);
        Date monthStart = Date.valueOf(currentMonth.atDay(1));
        Date nextMonthStart = Date.valueOf(
                currentMonth.plusMonths(1).atDay(1));
        List<String> customerNames = assignedCustomers.stream()
                .map(CustomerDTO::getCustomerName)
                .toList();
        List<MaintenanceCustomerAssignment> assignments =
                customerDAO.getAllMaintenanceCustomerAssignments();
        List<MaintenanceRecordDTO> currentMonthRecords =
                maintenanceDAO.getMaintenanceRecordsByMonth(
                        monthStart, nextMonthStart);
        List<MaintenanceRecordDTO> latestRecords =
                maintenanceDAO.getLatestMaintenanceRecordsByCustomers(
                        customerNames);
        return workInboxService.build(
                assignedCustomers,
                assignments,
                currentMonthRecords,
                latestRecords,
                today,
                WORK_INBOX_DISPLAY_LIMIT);
    }

    record ViewData(
            UserDTO user,
            List<MaintenanceRecordDTO> recentMaintenanceRecords,
            int maintenanceCount,
            List<TroubleshootingDTO> recentTroubleshootings,
            int troubleshootingCount,
            WorkInbox workInbox,
            List<UserVmHostDTO> vmHosts,
            int vmHostLimit,
            int vmHostCount) {
        ViewData {
            recentMaintenanceRecords = List.copyOf(
                    Objects.requireNonNull(
                            recentMaintenanceRecords,
                            "recentMaintenanceRecords"));
            recentTroubleshootings = List.copyOf(
                    Objects.requireNonNull(
                            recentTroubleshootings,
                            "recentTroubleshootings"));
            workInbox = Objects.requireNonNull(workInbox, "workInbox");
            vmHosts = List.copyOf(Objects.requireNonNull(vmHosts, "vmHosts"));
        }
    }
}
