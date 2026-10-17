package com.seck8s.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存预热单个 SKU（activity-service 传入）
 */
@Data
public class PreheatSku {
    private String skuId;
    private BigDecimal seckillPrice;
    private Integer stockLimit;
    private Integer quotaPerUser;
}
