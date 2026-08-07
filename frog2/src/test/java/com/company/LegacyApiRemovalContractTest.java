package com.company;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.config.ApplicationEnvironment;
import com.company.model.CustomerDAO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MeetingCommentDAO;
import com.company.model.MeetingCommentDTO;
import com.company.model.MeetingRecordDAO;
import com.company.model.MeetingRecordDTO;
import com.company.model.TroubleshootingDAO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.model.UserVmHostDAO;
import org.junit.jupiter.api.Test;

class LegacyApiRemovalContractTest {
    @Test
    void retiredInternalApisStayRemoved() {
        assertMissing(ApplicationEnvironment.class, "isDevelopment");
        assertMissing(ApplicationEnvironment.class, "isDevelopmentFeatureEnabled");
        assertMissing(UserDAO.class, "registerUser", UserDTO.class);
        assertMissing(
                UserDAO.class,
                "updateUserProfile",
                String.class,
                String.class,
                String.class);
        assertMissing(CustomerDAO.class, "getCustomerCount", String.class);
        assertMissing(CustomerDAO.class, "getCustomerCounts");
        assertMissing(
                MaintenanceRecordDAO.class,
                "getCustomersByInspector",
                String.class);
        assertMissing(
                MaintenanceRecordDAO.class,
                "getLicenseUsageSeries",
                String.class);
        assertMissing(
                MaintenanceRecordDAO.class,
                "getLatestMaintenanceRecordForCustomer",
                String.class);
        assertMissing(
                MaintenanceRecordDAO.class,
                "getMaintenanceRecordsByInspector",
                String.class);
        assertMissing(TroubleshootingDAO.class, "getAllTroubleshooting");
        assertMissing(
                TroubleshootingDAO.class,
                "searchTroubleshooting",
                String.class);
        assertMissing(
                TroubleshootingDAO.class,
                "getTroubleshootingByOwner",
                String.class,
                String.class);
        assertMissing(MeetingCommentDAO.class, "getComment", Long.class);
        assertMissing(
                MeetingRecordDAO.class,
                "updateMeetingRecord",
                MeetingRecordDTO.class);
        assertMissing(
                MeetingRecordDAO.class,
                "deleteMeetingRecord",
                Long.class);
        assertMissing(
                MeetingRecordDAO.class,
                "isAuthor",
                Long.class,
                String.class);
        assertMissing(
                MeetingCommentDAO.class,
                "updateComment",
                MeetingCommentDTO.class);
        assertMissing(
                MeetingCommentDAO.class,
                "deleteComment",
                Long.class);
        assertMissing(
                MeetingCommentDAO.class,
                "isCommentAuthor",
                Long.class,
                String.class);
        assertMissing(
                UserVmHostDAO.class,
                "countActiveHostsByOwner",
                String.class);
    }

    private static void assertMissing(
            Class<?> owner, String methodName, Class<?>... parameterTypes) {
        assertThrows(
                NoSuchMethodException.class,
                () -> owner.getMethod(methodName, parameterTypes),
                owner.getName() + "#" + methodName);
    }
}
