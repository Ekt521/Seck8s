package com.seck8s.order.dto;

import lombok.Data;

@Data
public class SeckillResult {
    /** CREATING 处理中 / SUCCESS 直接成功 / FAIL_SOLD_OUT 售罄 / FAIL_NOT_START 未开始 / FAIL_LIMIT 超限购 */
    private String status;
    private String orderId;
    private String message;

    public static SeckillResult creating(String orderId) {
        SeckillResult r = new SeckillResult();
        r.setStatus("CREATING");
        r.setOrderId(orderId);
        r.setMessage("订单处理中");
        return r;
    }

    public static SeckillResult fail(String status, String message) {
        SeckillResult r = new SeckillResult();
        r.setStatus(status);
        r.setMessage(message);
        return r;
    }
}
