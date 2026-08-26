package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import org.junit.jupiter.api.Test;

class MyPageQueryServiceTest {
    @Test
    void overviewLoadsOnlySummaryQueries() {
        UserDTO user = user("user-1");
        MaintenanceRecordDTO maintenance = new MaintenanceRecordDTO();
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        RecordingUserVmHostDAO hostDAO = new RecordingUserVmHostDAO();
        MyPageQueryService service = new MyPageQueryService(
                new StubUserDAO(user),
                new StubMaintenanceDAO(maintenance),
                new StubTroubleshootingDAO(troubleshooting),
                hostDAO);

        MyPageQueryService.ViewData result =
                service.loadOverview("user-1", 5);

        assertSame(user, result.user());
        assertEquals(List.of(maintenance), result.recentMaintenanceRecords());
        assertEquals(12, result.maintenanceCount());
        assertEquals(List.of(troubleshooting), result.recentTroubleshootings());
        assertEquals(7, result.troubleshootingCount());
        assertEquals(3, result.vmHostCount());
        assertEquals(20, result.vmHostLimit());
        assertEquals(0, hostDAO.listCalls);
        assertEquals(1, hostDAO.countCalls);
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
        MyPageQueryService service = new MyPageQueryService(
                new StubUserDAO(user),
                maintenanceDAO,
                troubleshootingDAO,
                hostDAO);

        MyPageQueryService.ViewData result = service.loadHosts("user-2");

        assertSame(user, result.user());
        assertEquals(List.of(first, second), result.vmHosts());
        assertEquals(2, result.vmHostCount());
        assertEquals(1, hostDAO.listCalls);
        assertEquals(0, hostDAO.countCalls);
        assertEquals(0, maintenanceDAO.calls);
        assertEquals(0, troubleshootingDAO.calls);
    }

    private static UserDTO user(String userId) {
        UserDTO user = new UserDTO();
        user.setUserId(userId);
        return user;
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

        @Override
        public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByOwner(
                String userId, int page, int pageSize) {
            calls++;
            return new PageResult<>(List.of(), 0, 1, pageSize);
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
