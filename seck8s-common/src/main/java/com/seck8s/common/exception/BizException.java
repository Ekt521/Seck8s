package com.seck8s.common.exception;

import com.seck8s.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一捕获转成 Result
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }
}
