package com.example.recruitment.common;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> failed(ResultCode code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code.getCode());
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> failed(String msg) {
        return failed(ResultCode.FAILED, msg);
    }

    public static <T> Result<T> validateFailed(String msg) {
        return failed(ResultCode.VALIDATE_FAILED, msg);
    }
}

