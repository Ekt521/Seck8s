package com.seck8s.activity.feign;

import com.seck8s.activity.dto.PreheatRequest;
import com.seck8s.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用库存服务：发布活动时触发库存预热
 */
@FeignClient(name = "inventory-service", contextId = "activityInventoryClient")
public interface InventoryClient {

    @PostMapping("/internal/inventory/preheat")
    Result<Void> preheat(@RequestBody PreheatRequest request);
}
