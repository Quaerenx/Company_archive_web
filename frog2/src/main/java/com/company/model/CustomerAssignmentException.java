package com.company.model;

public final class CustomerAssignmentException extends IllegalArgumentException {
    public CustomerAssignmentException() {
        super("A changed customer assignee must identify one registered user");
    }
}
