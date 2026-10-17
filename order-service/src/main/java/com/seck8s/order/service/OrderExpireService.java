package com.seck8s.order.service;

/**
 * 订单超时取消服务
 */
public interface OrderExpireService {

    /**
     * 订单建单成功后投递延迟消息（TTL=payTimeout 分钟后转死信 → order.expired → 幂等取消）
     */
    void sendExpireDelayMessage(String orderId);

    /**
     * 幂等取消订单（超时路径/定时扫描共用）：仅 UNPAID 可取消，成功触发库存回补
     * @return true 表示发生取消
     */
    boolean cancelExpiredOrder(String orderId);
}
