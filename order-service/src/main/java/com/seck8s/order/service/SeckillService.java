package com.seck8s.order.service;

import com.seck8s.order.dto.SeckillRequest;
import com.seck8s.order.dto.SeckillResult;

public interface SeckillService {

    /**
     * 秒杀入口：
     * 1. 校验活动开启（活动缓存）
     * 2. Feign 调 inventory 做 Redis Lua 原子扣减
     * 3. 成功则生成 orderId，事务内写本地消息表（order.created），返回"处理中"
     * 4. 前端轮询订单接口获取最终状态
     */
    SeckillResult seckill(Long userId, SeckillRequest request);
}
