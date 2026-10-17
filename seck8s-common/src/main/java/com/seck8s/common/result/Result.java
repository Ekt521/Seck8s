package com.seck8s.common.result;

import lombok.Data;

/**
 * 统一返回体
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(ResultCode rc) {
        return error(rc.getCode(), rc.getMessage());
    }

    public static <T> Result<T> error(ResultCode rc, String message) {
        return error(rc.getCode(), message);
    }
}
