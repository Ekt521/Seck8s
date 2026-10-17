package com.seck8s.inventory.service;

import com.seck8s.inventory.dto.PreheatRequest;

public interface InventoryService {

    /**
     * 秒杀扣减（第一道闸门）：Redis Lua 原子扣减库存 + 限购计数
     * @return 1成功 / -1未预热 / -2售罄 / -3超限购
     */
    int deduct(String activityId, String skuId, Long userId, Integer quantity);

    /**
     * DB 乐观锁扣减（第二道闸门）：幂等，MQ 消费 order.created 时调用
     */
    boolean dbDeduct(String orderId, String skuId, Integer quantity);

    /**
     * 库存回补（订单取消时）：Redis INCRBY + DB 乐观锁恢复，写 RESTORE 流水
     */
    void restore(String orderId, String skuId, Integer quantity);

    /**
     * 预热：写 Redis 库存 key（SET NX 只预热一次）+ 写活动/SKU 缓存 + DB 初始化
     */
    void preheat(PreheatRequest request);

    /** 查询 SKU 实时库存余量（先 Redis，缺失回源 DB） */
    Integer getStock(String skuId);
}
