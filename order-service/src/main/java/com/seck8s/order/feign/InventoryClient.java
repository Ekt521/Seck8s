package com.seck8s.order.feign;

import com.seck8s.common.result.Result;
import com.seck8s.order.dto.DeductRequest;
import com.seck8s.order.dto.DeductResponse;
import com.seck8s.order.dto.RestoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用库存服务：秒杀热路径扣减
 */
@FeignClient(name = "inventory-service", contextId = "orderInventoryClient")
public interface InventoryClient {

    @PostMapping("/internal/inventory/deduct")
    DeductResponse deduct(@RequestBody DeductRequest request);

    @PostMapping("/internal/inventory/restore")
    void restore(@RequestBody RestoreRequest request);
}
