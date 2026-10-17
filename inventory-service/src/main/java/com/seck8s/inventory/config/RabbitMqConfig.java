package com.seck8s.inventory.config;

import com.seck8s.common.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存服务 RabbitMQ 拓扑：声明交换机 + 本服务要消费的队列 + 绑定
 */
@Configuration
public class RabbitMqConfig {

    /** 订单事件交换机（topic，兼作死信交换机） */
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /** 订单创建队列：DB 乐观锁扣减 */
    @Bean
    public Queue stockDeductQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_STOCK_DEDUCT).build();
    }

    /** 订单取消队列：库存回补（独立队列，与通知服务各自消费 order.cancelled 实现 pub/sub） */
    @Bean
    public Queue stockRestoreQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_STOCK_RESTORE).build();
    }

    @Bean
    public Binding stockDeductBinding(TopicExchange orderExchange, Queue stockDeductQueue) {
        return BindingBuilder.bind(stockDeductQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding orderCancelBinding(TopicExchange orderExchange, Queue stockRestoreQueue) {
        return BindingBuilder.bind(stockRestoreQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_CANCELLED);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
