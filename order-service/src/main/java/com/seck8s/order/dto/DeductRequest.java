package com.seck8s.order.dto;

import lombok.Data;

@Data
public class DeductRequest {
    private String activityId;
    private String skuId;
    private Long userId;
    private Integer quantity;
}
