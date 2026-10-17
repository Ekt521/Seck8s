package com.seck8s.common.event;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付结果事件（payment-service 发件箱 payload）
 */
@Data
public class PayResultEvent {
    private String paymentId;
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    /** SUCCESS / FAILED */
    private String result;
}
