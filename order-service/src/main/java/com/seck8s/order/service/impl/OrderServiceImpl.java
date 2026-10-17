package com.seck8s.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.order.entity.MessageOutbox;
import com.seck8s.order.entity.Order;
import com.seck8s.order.mapper.OrderMapper;
import com.seck8s.order.service.OrderService;
import com.seck8s.order.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务核心：幂等建单/取消/支付结果，状态受限更新保证库存只回补/扣一次
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OutboxService outboxService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderServiceImpl(OutboxService outboxService, StringRedisTemplate redisTemplate) {
        this.outboxService = outboxService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Order getOrderByOrderId(String orderId) {
        return this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
    }

    @Override
    public List<Order> listUserOrders(Long userId, String status) {
        return this.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null && !status.isBlank(), Order::getOrderStatus, status)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        Order exist = getOrderByOrderId(order.getOrderId());
        if (exist != null) {
            return exist; // 幂等：重复消息直接返回已有订单
        }
        order.setOrderStatus("UNPAID");
        this.save(order);
        return order;
    }

    @Override
    @Transactional
    public boolean cancelOrder(String orderId, Long userId) {
        Order order = getOrderByOrderId(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return false; // 不存在或非本人
        }
        int updated = this.baseMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getOrderStatus, "UNPAID")
                .set(Order::getOrderStatus, "CANCELLED"));
        if (updated > 0) {
            // 状态受限更新成功才发取消事件 → 库存回补（保证只回补一次）
            publishOrderEvent(order, MqConstants.BIZ_ORDER_CANCEL, MqConstants.ROUTING_ORDER_CANCELLED, "ORDER_CANCEL");
            redisTemplate.opsForValue().set(RedisKeys.seckillResultKey(orderId), "CANCELLED", Duration.ofHours(2));
        }
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean handlePayResult(String orderId, boolean success) {
        if (!success) {
            // 支付失败：订单保持 UNPAID，由超时取消机制兜底
            return false;
        }
        Order order = getOrderByOrderId(orderId);
        if (order == null) {
            return false;
        }
        int updated = this.baseMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getOrderStatus, "UNPAID")
                .set(Order::getOrderStatus, "PAID"));
        if (updated > 0) {
            publishOrderEvent(order, MqConstants.BIZ_ORDER_PAID, MqConstants.ROUTING_ORDER_PAID, "ORDER_PAID");
            redisTemplate.opsForValue().set(RedisKeys.seckillResultKey(orderId), "PAID", Duration.ofHours(2));
        }
        return updated > 0;
    }

    /** 发件箱写入订单事件（状态变更后调用） */
    private void publishOrderEvent(Order order, String bizType, String routingKey, String eventType) {
        try {
            OrderEvent event = new OrderEvent();
            event.setOrderId(order.getOrderId());
            event.setUserId(order.getUserId());
            event.setActivityId(order.getActivityId());
            event.setSkuId(order.getSkuId());
            event.setTicketId(order.getTicketId());
            event.setQuantity(order.getQuantity());
            event.setAmount(order.getAmount());
            event.setEventType(eventType);

            MessageOutbox msg = new MessageOutbox();
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setBizType(bizType);
            msg.setBizKey(order.getOrderId());
            msg.setExchange(MqConstants.ORDER_EXCHANGE);
            msg.setRoutingKey(routingKey);
            msg.setPayload(objectMapper.writeValueAsString(event));
            outboxService.save(msg);
        } catch (Exception e) {
            log.error("写入订单事件发件箱失败 orderId={}", order.getOrderId(), e);
        }
    }
}
