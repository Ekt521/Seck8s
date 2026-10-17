package com.seck8s.common.constant;

/**
 * Redis 键设计（全链路统一约定）
 */
public final class RedisKeys {

    private static final String PREFIX = "seck8s:";

    /** 剩余库存：seck8s:stock:{skuId} -> string(数量)，预热写入 */
    public static String stockKey(String skuId) {
        return PREFIX + "stock:" + skuId;
    }

    /** 用户限购计数：seck8s:bought:{activityId}:{userId} -> string(已购数)，TTL=活动结束 */
    public static String boughtKey(String activityId, Long userId) {
        return PREFIX + "bought:" + activityId + ":" + userId;
    }

    /** 活动缓存：seck8s:activity:{activityId} -> hash */
    public static String activityKey(String activityId) {
        return PREFIX + "activity:" + activityId;
    }

    /** SKU缓存：seck8s:sku:{skuId} -> hash */
    public static String skuKey(String skuId) {
        return PREFIX + "sku:" + skuId;
    }

    /** 秒杀结果缓存：seck8s:seckill:result:{orderId} -> CREATING/CREATED/PAID/CANCELLED */
    public static String seckillResultKey(String orderId) {
        return PREFIX + "seckill:result:" + orderId;
    }

    /** 秒杀接口限流：seck8s:rate:{userId}:{activityId}（网关用） */
    public static String rateKey(Long userId, String activityId) {
        return PREFIX + "rate:" + userId + ":" + activityId;
    }

    private RedisKeys() {
    }
}
