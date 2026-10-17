package com.seck8s.activity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 预热单个 SKU 的请求体（发给 inventory-service）
 */
@Data
public class PreheatSku {
    private String skuId;
    private BigDecimal seckillPrice;
    private Integer stockLimit;
    private Integer quotaPerUser;
}
