package com.seck8s.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequest {
    private String orderId;
    private String skuId;
    private Integer quantity;
}
