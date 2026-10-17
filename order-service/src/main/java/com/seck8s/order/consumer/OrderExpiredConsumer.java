package com.seck8s.order.consumer;

import com.rabbitmq.client.Channel;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.order.service.OrderExpireService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时消费者：消费 DLX 死信（order.expired），幂等取消订单
 */
@Slf4j
@Component
public class OrderExpiredConsumer {

    private final OrderExpireService orderExpireService;

    public OrderExpiredConsumer(OrderExpireService orderExpireService) {
        this.orderExpireService = orderExpireService;
    }

    @RabbitListener(queues = MqConstants.QUEUE_ORDER_EXPIRED)
    public void onExpired(Message message, Channel channel) throws Exception {
        String orderId = new String(message.getBody());
        try {
            orderExpireService.cancelExpiredOrder(orderId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("超时取消处理失败 orderId={}", orderId, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
