package com.seck8s.notification.config;

import com.seck8s.common.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务 RabbitMQ 拓扑：声明交换机 + 本服务要消费的队列 + 绑定
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange payExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.PAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /** 订单取消通知队列（独立队列，与 inventory 回补各自消费 order.cancelled） */
    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_CANCEL_NOTIFY).build();
    }

    /** 支付成功通知队列 */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_PAID).build();
    }

    /** 支付结果通知队列（独立队列，与 order 更新各自消费 pay.result） */
    @Bean
    public Queue payResultQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_PAY_RESULT_NOTIFY).build();
    }

    @Bean
    public Binding orderCancelBinding(TopicExchange orderExchange, Queue orderCancelQueue) {
        return BindingBuilder.bind(orderCancelQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_CANCELLED);
    }

    @Bean
    public Binding orderPaidBinding(TopicExchange orderExchange, Queue orderPaidQueue) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_PAID);
    }

    @Bean
    public Binding payResultBinding(TopicExchange payExchange, Queue payResultQueue) {
        return BindingBuilder.bind(payResultQueue).to(payExchange).with(MqConstants.ROUTING_PAY_RESULT);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
