package com.gm.warning.model;

public class CreateLeaveResult {
    private final boolean success;
    private final String message;
    private final int statusCode;

    public CreateLeaveResult(boolean success, String message, int statusCode) {
        this.success = success;
        this.message = message;
        this.statusCode = statusCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
