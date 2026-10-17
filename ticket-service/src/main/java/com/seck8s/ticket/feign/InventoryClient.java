package com.seck8s.ticket.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调用库存服务：查询 SKU 实时库存余量（用于查票列表展示）
 */
@FeignClient(name = "inventory-service", contextId = "ticketInventoryClient")
public interface InventoryClient {

    @GetMapping("/internal/inventory/stock/{skuId}")
    Integer getStock(@PathVariable("skuId") String skuId);
}
