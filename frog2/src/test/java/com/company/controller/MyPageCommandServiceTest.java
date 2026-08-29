package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.MonthlyCustomerResponseDAO;
import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import org.junit.jupiter.api.Test;

class MyPageCommandServiceTest {
    @Test
    void passwordPolicyFailureDoesNotAuthenticateOrWrite() {
        StubUserDAO userDAO = new StubUserDAO();
        MyPageCommandService service = service(userDAO, new StubMonthlyDAO());

        MyPageCommandService.PasswordChangeResult result =
                service.updatePassword(
                        "user-1",
                        new MyPageCommandService.PasswordChange(
                                "current", "short", "short"));

        assertEquals(
                MyPageCommandService.PasswordChangeStatus.INVALID,
                result.status());
        assertEquals(
                "새 비밀번호는 최소 8자 이상이어야 합니다.",
                result.errorMessage());
        assertEquals(0, userDAO.authenticateCalls);
        assertEquals(0, userDAO.updatePasswordCalls);
    }

    @Test
    void passwordChangeAuthenticatesBeforeWriting() {
        StubUserDAO userDAO = new StubUserDAO();
        MyPageCommandService service = service(userDAO, new StubMonthlyDAO());
        MyPageCommandService.PasswordChange change =
                new MyPageCommandService.PasswordChange(
                        "old-password", "new-password", "new-password");

        MyPageCommandService.PasswordChangeResult rejected =
                service.updatePassword("user-1", change);
        assertEquals(
                MyPageCommandService.PasswordChangeStatus.WRONG_CURRENT_PASSWORD,
                rejected.status());
        assertEquals(0, userDAO.updatePasswordCalls);

        userDAO.authenticatedUser = new UserDTO(
                "user-1", "", "테스터", "QA");
        userDAO.updatePasswordResult = true;
        MyPageCommandService.PasswordChangeResult updated =
                service.updatePassword("user-1", change);
        assertEquals(
                MyPageCommandService.PasswordChangeStatus.SUCCESS,
                updated.status());
        assertEquals("old-password", userDAO.authenticatedPassword);
        assertEquals("new-password", userDAO.updatedPassword);
    }

    @Test
    void delegatesProfileAndOwnedMonthlyWritesToTheirDaos() {
        StubUserDAO userDAO = new StubUserDAO();
        StubMonthlyDAO monthlyDAO = new StubMonthlyDAO();
        userDAO.updateNameResult = true;
        monthlyDAO.writeResult = true;
        MyPageCommandService service = service(userDAO, monthlyDAO);
        MonthlyCustomerResponseDTO response = new MonthlyCustomerResponseDTO();

        assertTrue(service.updateProfile("user-1", "새 이름"));
        assertEquals("user-1", userDAO.updatedUserId);
        assertEquals("새 이름", userDAO.updatedUserName);

        assertTrue(service.addMonthlyResponse(response));
        assertSame(response, monthlyDAO.addedResponse);
        assertTrue(service.updateMonthlyResponse(response));
        assertSame(response, monthlyDAO.updatedResponse);
        assertTrue(service.deleteMonthlyResponse(17, "user-1"));
        assertEquals(17, monthlyDAO.deletedResponseId);
        assertEquals("user-1", monthlyDAO.deletedOwnerId);
    }

    private static MyPageCommandService service(
            StubUserDAO userDAO, StubMonthlyDAO monthlyDAO) {
        return new MyPageCommandService(userDAO, monthlyDAO);
    }

    private static final class StubUserDAO extends UserDAO {
        private boolean updateNameResult;
        private boolean updatePasswordResult;
        private UserDTO authenticatedUser;
        private int authenticateCalls;
        private int updatePasswordCalls;
        private String updatedUserId;
        private String updatedUserName;
        private String authenticatedPassword;
        private String updatedPassword;

        @Override
        public boolean updateUserName(String userId, String userName) {
            updatedUserId = userId;
            updatedUserName = userName;
            return updateNameResult;
        }

        @Override
        public UserDTO authenticateUser(String userId, String password) {
            authenticateCalls++;
            authenticatedPassword = password;
            return authenticatedUser;
        }

        @Override
        public boolean updatePassword(String userId, String newPassword) {
            updatePasswordCalls++;
            updatedPassword = newPassword;
            return updatePasswordResult;
        }
    }

    private static final class StubMonthlyDAO
            extends MonthlyCustomerResponseDAO {
        private boolean writeResult;
        private MonthlyCustomerResponseDTO addedResponse;
        private MonthlyCustomerResponseDTO updatedResponse;
        private int deletedResponseId;
        private String deletedOwnerId;

        @Override
        public boolean addResponse(MonthlyCustomerResponseDTO response) {
            addedResponse = response;
            return writeResult;
        }

        @Override
        public boolean updateResponse(MonthlyCustomerResponseDTO response) {
            updatedResponse = response;
            return writeResult;
        }

        @Override
        public boolean deleteResponse(int responseId, String userId) {
            deletedResponseId = responseId;
            deletedOwnerId = userId;
            return writeResult;
        }
    }
}
