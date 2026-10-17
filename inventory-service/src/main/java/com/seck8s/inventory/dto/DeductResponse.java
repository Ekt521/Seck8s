package com.seck8s.inventory.dto;

import lombok.Data;

/**
 * 扣减结果：code 1成功 / -1未预热 / -2售罄 / -3超限购
 */
@Data
public class DeductResponse {
    private int code;
    private String message;

    public static DeductResponse of(int code, String message) {
        DeductResponse r = new DeductResponse();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
