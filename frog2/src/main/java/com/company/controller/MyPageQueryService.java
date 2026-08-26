package com.company.controller;

import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import com.company.model.UserVmHostDTO;
import java.util.List;
import java.util.Objects;

final class MyPageQueryService {
    private final UserDAO userDAO;
    private final MaintenanceRecordDAO maintenanceDAO;
    private final TroubleshootingDAO troubleshootingDAO;
    private final UserVmHostDAO userVmHostDAO;

    MyPageQueryService() {
        this(
                new UserDAO(),
                new MaintenanceRecordDAO(),
                new TroubleshootingDAO(),
                new UserVmHostDAO());
    }

    MyPageQueryService(
            UserDAO userDAO,
            MaintenanceRecordDAO maintenanceDAO,
            TroubleshootingDAO troubleshootingDAO,
            UserVmHostDAO userVmHostDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
        this.userVmHostDAO = Objects.requireNonNull(
                userVmHostDAO, "userVmHostDAO");
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
        return new ViewData(
                user,
                maintenance.items(),
                maintenance.totalCount(),
                troubleshooting.items(),
                troubleshooting.totalCount(),
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
                hosts,
                hostLimit,
                hosts.size());
    }

    record ViewData(
            UserDTO user,
            List<MaintenanceRecordDTO> recentMaintenanceRecords,
            int maintenanceCount,
            List<TroubleshootingDTO> recentTroubleshootings,
            int troubleshootingCount,
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
            vmHosts = List.copyOf(Objects.requireNonNull(vmHosts, "vmHosts"));
        }
    }
}
