package com.seck8s.order.dto;

import lombok.Data;

@Data
public class SeckillRequest {
    private String activityId;
    private String skuId;
    private Integer quantity = 1;
}
