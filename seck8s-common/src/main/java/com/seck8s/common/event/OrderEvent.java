package com.seck8s.common.event;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单事件消息体（发件箱 payload，跨服务 MQ 传递）
 * bizType: ORDER_CREATE / ORDER_CANCEL / ORDER_PAID
 */
@Data
public class OrderEvent {
    private String orderId;
    private Long userId;
    private String activityId;
    private String skuId;
    private String ticketId;
    private Integer quantity;
    private BigDecimal amount;
    /** ORDER_CREATE / ORDER_CANCEL / ORDER_PAID / ORDER_EXPIRED */
    private String eventType;
}
