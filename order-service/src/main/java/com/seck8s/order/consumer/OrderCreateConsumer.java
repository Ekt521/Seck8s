package com.seck8s.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.constant.RedisKeys;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.order.entity.Order;
import com.seck8s.order.service.OrderExpireService;
import com.seck8s.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 订单创建消费者：幂等落库订单（uk_order_id 去重），写秒杀结果缓存
 */
@Slf4j
@Component
public class OrderCreateConsumer {

    private final OrderService orderService;
    private final OrderExpireService orderExpireService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderCreateConsumer(OrderService orderService,
                               OrderExpireService orderExpireService,
                               StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.orderExpireService = orderExpireService;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = MqConstants.QUEUE_ORDER_CREATE)
    public void onMessage(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody());
        OrderEvent event;
        try {
            event = objectMapper.readValue(body, OrderEvent.class);
        } catch (Exception e) {
            // 消息体解析失败：直接确认，避免死循环
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.error("order.created 消息解析失败: {}", body);
            return;
        }

        try {
            Order order = new Order();
            order.setOrderId(event.getOrderId());
            order.setUserId(event.getUserId());
            order.setActivityId(event.getActivityId());
            order.setSkuId(event.getSkuId());
            order.setTicketId(event.getTicketId());
            order.setQuantity(event.getQuantity());
            order.setAmount(event.getAmount());
            order.setExpireTime(LocalDateTime.now().plusMinutes(15)); // 15 分钟支付超时
            Order exist = orderService.createOrder(order);
            // 写秒杀结果缓存
            redisTemplate.opsForValue().set(RedisKeys.seckillResultKey(event.getOrderId()),
                    exist.getOrderStatus(), java.time.Duration.ofHours(2));
            // 投递超时延迟消息（DLX+TTL → order.expired → 幂等取消）
            orderExpireService.sendExpireDelayMessage(event.getOrderId());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单落库成功 orderId={} status={}", event.getOrderId(), exist.getOrderStatus());
        } catch (Exception e) {
            log.error("订单落库失败 orderId={}: {}", event.getOrderId(), e.getMessage());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
