package com.seck8s.inventory.dto;

import lombok.Data;

import java.util.List;

/**
 * 库存预热请求体：一个活动下的全部参与 SKU
 */
@Data
public class PreheatRequest {
    private String activityId;
    private List<PreheatSku> skus;
}
