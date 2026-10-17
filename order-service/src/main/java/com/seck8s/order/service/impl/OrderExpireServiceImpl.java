package com.seck8s.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.order.entity.MessageOutbox;
import com.seck8s.order.entity.Order;
import com.seck8s.order.mapper.OrderMapper;
import com.seck8s.order.service.OrderExpireService;
import com.seck8s.order.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * 超时取消实现：
 * 主路径：DLX + TTL 延迟消息（建单后投递）
 * 兜底：定时扫描（与延迟消息共用 cancelExpiredOrder，天然幂等）
 */
@Slf4j
@Service
public class OrderExpireServiceImpl implements OrderExpireService {

    private final OrderMapper orderMapper;
    private final OutboxService outboxService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${seck8s.order.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    public OrderExpireServiceImpl(OrderMapper orderMapper,
                                  OutboxService outboxService,
                                  StringRedisTemplate redisTemplate,
                                  RabbitTemplate rabbitTemplate) {
        this.orderMapper = orderMapper;
        this.outboxService = outboxService;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendExpireDelayMessage(String orderId) {
        // 投递到 delay 队列：per-message TTL = payTimeout 分钟，到期转死信 order.expired
        try {
            rabbitTemplate.convertAndSend(
                    "", MqConstants.QUEUE_ORDER_DELAY, orderId,
                    m -> {
                        m.getMessageProperties().setExpiration(String.valueOf(payTimeoutMinutes * 60_000L));
                        return m;
                    });
            log.info("已投递订单超时延迟消息 orderId={} ttl={}min", orderId, payTimeoutMinutes);
        } catch (Exception e) {
            log.error("投递超时延迟消息失败 orderId={}，将由定时扫描兜底", orderId, e);
        }
    }

    @Override
    @Transactional
    public boolean cancelExpiredOrder(String orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderId, orderId));
        if (order == null) {
            return false;
        }
        // 幂等：仅 UNPAID 可取消
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getOrderStatus, "UNPAID")
                .set(Order::getOrderStatus, "CANCELLED"));
        if (updated > 0) {
            // 状态受限更新成功 → 写取消事件 → 库存回补
            try {
                OrderEvent event = new OrderEvent();
                event.setOrderId(order.getOrderId());
                event.setUserId(order.getUserId());
                event.setActivityId(order.getActivityId());
                event.setSkuId(order.getSkuId());
                event.setQuantity(order.getQuantity());
                event.setEventType("ORDER_CANCEL");

                MessageOutbox msg = new MessageOutbox();
                msg.setMsgId(UUID.randomUUID().toString());
                msg.setBizType(MqConstants.BIZ_ORDER_CANCEL);
                msg.setBizKey(orderId);
                msg.setExchange(MqConstants.ORDER_EXCHANGE);
                msg.setRoutingKey(MqConstants.ROUTING_ORDER_CANCELLED);
                msg.setPayload(objectMapper.writeValueAsString(event));
                outboxService.save(msg);
            } catch (Exception e) {
                log.error("超时取消写发件箱失败 orderId={}", orderId, e);
            }
            redisTemplate.opsForValue().set(RedisKeys.seckillResultKey(orderId), "CANCELLED", Duration.ofHours(2));
            log.info("订单超时取消 orderId={}", orderId);
            return true;
        }
        return false;
    }
}
