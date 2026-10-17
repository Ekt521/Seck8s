package com.seck8s.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.common.event.PayResultEvent;
import com.seck8s.notification.entity.Notification;
import com.seck8s.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 订单/支付事件通知消费者：异步记录并发送站内通知
 */
@Slf4j
@Component
public class OrderNotifyConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderNotifyConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = MqConstants.QUEUE_ORDER_CANCEL_NOTIFY)
    public void onOrderCancelled(Message message, Channel channel) throws Exception {
        OrderEvent event = objectMapper.readValue(new String(message.getBody()), OrderEvent.class);
        try {
            Notification n = base(event.getOrderId(), event.getUserId(), "ORDER_CANCELLED",
                    "您的订单 " + event.getOrderId() + " 已取消，库存已释放");
            notificationService.notify(n);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("取消通知处理失败 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    @RabbitListener(queues = MqConstants.QUEUE_ORDER_PAID)
    public void onOrderPaid(Message message, Channel channel) throws Exception {
        OrderEvent event = objectMapper.readValue(new String(message.getBody()), OrderEvent.class);
        try {
            Notification n = base(event.getOrderId(), event.getUserId(), "ORDER_PAID",
                    "您的订单 " + event.getOrderId() + " 已支付成功，请按时观演");
            notificationService.notify(n);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("支付成功通知处理失败 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    @RabbitListener(queues = MqConstants.QUEUE_PAY_RESULT_NOTIFY)
    public void onPayResult(Message message, Channel channel) throws Exception {
        PayResultEvent event = objectMapper.readValue(new String(message.getBody()), PayResultEvent.class);
        try {
            if ("SUCCESS".equals(event.getResult())) {
                Notification n = base(event.getOrderId(), event.getUserId(), "ORDER_PAID",
                        "您的订单 " + event.getOrderId() + " 支付成功（金额 ¥" + event.getAmount() + "）");
                notificationService.notify(n);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("支付结果通知处理失败 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    private Notification base(String orderId, Long userId, String type, String content) {
        Notification n = new Notification();
        n.setNotifyId(UUID.randomUUID().toString());
        n.setUserId(userId);
        n.setOrderId(orderId);
        n.setType(type);
        n.setChannel("STATION");
        n.setContent(content);
        return n;
    }
}
