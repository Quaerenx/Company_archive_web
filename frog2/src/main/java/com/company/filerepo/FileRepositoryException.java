package com.company.filerepo;

public class FileRepositoryException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final String code;

    public FileRepositoryException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public FileRepositoryException(int httpStatus, String code, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
