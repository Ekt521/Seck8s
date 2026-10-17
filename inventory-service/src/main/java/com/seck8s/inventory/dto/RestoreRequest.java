package com.seck8s.inventory.dto;

import lombok.Data;

/**
 * 库存回补请求（订单取消时调用）
 */
@Data
public class RestoreRequest {
    private String orderId;
    private String skuId;
    private Integer quantity;
}
