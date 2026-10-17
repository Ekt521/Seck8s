package com.seck8s.inventory.dto;

import lombok.Data;

/**
 * 秒杀扣减请求（order-service → inventory）
 */
@Data
public class DeductRequest {
    private String activityId;
    private String skuId;
    private Long userId;
    private Integer quantity;
}
