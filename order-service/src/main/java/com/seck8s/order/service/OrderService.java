package com.seck8s.order.service;

import com.seck8s.order.entity.Order;

import java.util.List;

public interface OrderService {

    Order getOrderByOrderId(String orderId);

    List<Order> listUserOrders(Long userId, String status);

    /** 幂等建单：uk_order_id 唯一索引，重复插入返回已有订单 */
    Order createOrder(Order order);

    /** 取消订单（幂等）：仅 UNPAID 可取消；发生状态变更返回 true（触发 ORDER_CANCEL 事件回补库存） */
    boolean cancelOrder(String orderId, Long userId);

    /** 支付结果处理（幂等）：仅 UNPAID 可置 PAID；发生状态变更返回 true（触发 ORDER_PAID 事件） */
    boolean handlePayResult(String orderId, boolean success);
}
