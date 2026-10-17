package com.seck8s.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.event.PayResultEvent;
import com.seck8s.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 支付结果消费者：幂等更新订单状态（仅 UNPAID → PAID），成功则发 ORDER_PAID 事件
 */
@Slf4j
@Component
public class PayResultConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PayResultConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = MqConstants.QUEUE_PAY_RESULT_ORDER)
    public void onPayResult(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody());
        PayResultEvent event;
        try {
            event = objectMapper.readValue(body, PayResultEvent.class);
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.error("pay.result 消息解析失败: {}", body);
            return;
        }

        try {
            boolean success = "SUCCESS".equals(event.getResult());
            boolean changed = orderService.handlePayResult(event.getOrderId(), success);
            if (!success) {
                log.info("支付失败 orderId={}，订单保持待支付，将由超时取消兜底", event.getOrderId());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("支付结果处理失败 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
