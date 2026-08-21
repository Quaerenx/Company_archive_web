package com.company.customerhistory;

public final class CustomerHistoryStorageException extends RuntimeException {
    public CustomerHistoryStorageException(String message) {
        super(message);
    }

    public CustomerHistoryStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
