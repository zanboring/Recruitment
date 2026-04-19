package com.example.recruitment.common;

public enum ResultCode {
    SUCCESS(0, "success"),
    FAILED(1, "failed"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    VALIDATE_FAILED(422, "validate failed"),
    SERVER_ERROR(500, "server error");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
}