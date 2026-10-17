package com.seck8s.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seck8s.common.constant.MqConstants;
import com.seck8s.common.event.OrderEvent;
import com.seck8s.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 库存事件消费者：
 * - order.created  → DB 乐观锁扣减（第二道闸门），失败则返回 false 触发补偿
 * - order.cancelled → Redis + DB 库存回补
 */
@Slf4j
@Component
public class StockConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(queues = MqConstants.QUEUE_STOCK_DEDUCT)
    public void onOrderCreated(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody());
        OrderEvent event = objectMapper.readValue(body, OrderEvent.class);
        try {
            boolean ok = inventoryService.dbDeduct(event.getOrderId(), event.getSkuId(), event.getQuantity());
            if (ok) {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // DB 库存不足（Redis 与 DB 不一致的极端情况）：ack 丢弃并由对账兜底，
                // 避免无限重试阻塞队列
                log.error("DB乐观锁扣减失败 orderId={} skuId={}，需对账介入", event.getOrderId(), event.getSkuId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            log.error("DB扣减异常 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    @RabbitListener(queues = MqConstants.QUEUE_STOCK_RESTORE)
    public void onOrderCancelled(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody());
        OrderEvent event = objectMapper.readValue(body, OrderEvent.class);
        try {
            inventoryService.restore(event.getOrderId(), event.getSkuId(), event.getQuantity());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单取消回补库存 orderId={} skuId={} qty={}", event.getOrderId(), event.getSkuId(), event.getQuantity());
        } catch (Exception e) {
            log.error("库存回补异常 orderId={}", event.getOrderId(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
