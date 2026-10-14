package com.company.model;

public final class CustomerEditConflictException extends RuntimeException {
    public CustomerEditConflictException() {
        super("Customer details changed after the edit form was opened");
    }
}
