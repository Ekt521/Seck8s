package com.seck8s.inventory.controller;

import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.result.Result;
import com.seck8s.inventory.dto.DeductRequest;
import com.seck8s.inventory.dto.DeductResponse;
import com.seck8s.inventory.dto.PreheatRequest;
import com.seck8s.inventory.dto.RestoreRequest;
import com.seck8s.inventory.service.InventoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 库存服务内部接口（由 order-service / activity-service / ticket-service 通过 Feign 调用）
 */
@RestController
@RequestMapping("/internal/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StringRedisTemplate redisTemplate;

    public InventoryController(InventoryService inventoryService, StringRedisTemplate redisTemplate) {
        this.inventoryService = inventoryService;
        this.redisTemplate = redisTemplate;
    }

    /** 秒杀扣减（Redis Lua） */
    @PostMapping("/deduct")
    public DeductResponse deduct(@RequestBody DeductRequest request) {
        int code = inventoryService.deduct(request.getActivityId(), request.getSkuId(),
                request.getUserId(), request.getQuantity());
        return switch (code) {
            case 1 -> DeductResponse.of(1, "扣减成功");
            case -1 -> DeductResponse.of(-1, "活动未开始或未预热");
            case -2 -> DeductResponse.of(-2, "已售罄");
            case -3 -> DeductResponse.of(-3, "超出限购数量");
            default -> DeductResponse.of(code, "扣减失败");
        };
    }

    /** 库存回补（订单取消时，Feign 直调或 MQ 兜底） */
    @PostMapping("/restore")
    public Result<Void> restore(@RequestBody RestoreRequest request) {
        inventoryService.restore(request.getOrderId(), request.getSkuId(), request.getQuantity());
        return Result.ok();
    }

    /** 库存预热（活动发布时） */
    @PostMapping("/preheat")
    public Result<Void> preheat(@RequestBody PreheatRequest request) {
        inventoryService.preheat(request);
        return Result.ok();
    }

    /** 查询实时库存余量（查票列表用） */
    @GetMapping("/stock/{skuId}")
    public Integer getStock(@PathVariable String skuId) {
        return inventoryService.getStock(skuId);
    }
}
