package com.company.controller;

import com.company.model.MonthlyCustomerResponseDAO;
import com.company.model.MonthlyCustomerResponseDTO;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.security.PasswordPolicy;
import java.util.Objects;
import java.util.Optional;

final class MyPageCommandService {
    private final UserDAO userDAO;
    private final MonthlyCustomerResponseDAO monthlyResponseDAO;

    MyPageCommandService(
            UserDAO userDAO,
            MonthlyCustomerResponseDAO monthlyResponseDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
        this.monthlyResponseDAO = Objects.requireNonNull(
                monthlyResponseDAO, "monthlyResponseDAO");
    }

    boolean updateProfile(String userId, String userName) {
        return userDAO.updateUserName(userId, userName);
    }

    PasswordChangeResult updatePassword(
            String userId, PasswordChange change) {
        Objects.requireNonNull(change, "change");
        Optional<String> validationError = PasswordPolicy.validate(
                change.currentPassword(),
                change.newPassword(),
                change.confirmation());
        if (validationError.isPresent()) {
            return new PasswordChangeResult(
                    PasswordChangeStatus.INVALID, validationError.get());
        }

        UserDTO authenticatedUser = userDAO.authenticateUser(
                userId, change.currentPassword());
        if (authenticatedUser == null) {
            return new PasswordChangeResult(
                    PasswordChangeStatus.WRONG_CURRENT_PASSWORD,
                    "현재 비밀번호가 올바르지 않습니다.");
        }

        boolean updated = userDAO.updatePassword(
                userId, change.newPassword());
        return updated
                ? new PasswordChangeResult(
                        PasswordChangeStatus.SUCCESS, null)
                : new PasswordChangeResult(
                        PasswordChangeStatus.WRITE_FAILED,
                        "비밀번호 변경에 실패했습니다.");
    }

    boolean addMonthlyResponse(MonthlyCustomerResponseDTO response) {
        return monthlyResponseDAO.addResponse(response);
    }

    boolean updateMonthlyResponse(MonthlyCustomerResponseDTO response) {
        return monthlyResponseDAO.updateResponse(response);
    }

    boolean deleteMonthlyResponse(int responseId, String userId) {
        return monthlyResponseDAO.deleteResponse(responseId, userId);
    }

    enum PasswordChangeStatus {
        SUCCESS,
        INVALID,
        WRONG_CURRENT_PASSWORD,
        WRITE_FAILED
    }

    record PasswordChangeResult(
            PasswordChangeStatus status, String errorMessage) {
    }

    record PasswordChange(
            String currentPassword,
            String newPassword,
            String confirmation) {
    }
}
