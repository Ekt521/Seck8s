package com.seck8s.order.config;

import com.seck8s.common.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务 RabbitMQ 拓扑：交换机 + 队列 + 绑定 + DLX 延迟队列
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

    /** 支付事件交换机 */
    @Bean
    public TopicExchange payExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.PAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /** 订单创建队列：order-service 幂等建单 */
    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_CREATE).build();
    }

    /** 订单创建队列：inventory-service DB 乐观锁扣减 */
    @Bean
    public Queue stockDeductQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_STOCK_DEDUCT).build();
    }

    /** 支付成功队列：notification */
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_PAID).build();
    }

    /** 支付结果队列：order-service 更新订单状态（独立队列，与 notification 各自消费 pay.result） */
    @Bean
    public Queue payResultQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_PAY_RESULT_ORDER).build();
    }

    /** 超时延迟队列：per-message TTL，到期转死信 → order.exchange(order.expired) */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_DELAY)
                .ttl(900_000)   // 队列默认 TTL 15 分钟（每条消息可用 header 覆盖）
                .deadLetterExchange(MqConstants.ORDER_EXCHANGE)
                .deadLetterRoutingKey(MqConstants.ROUTING_ORDER_EXPIRED)
                .build();
    }

    /** 超时取消队列：order-service 幂等取消 */
    @Bean
    public Queue orderExpiredQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_ORDER_EXPIRED).build();
    }

    @Bean
    public Binding orderCreateBinding(TopicExchange orderExchange, Queue orderCreateQueue) {
        return BindingBuilder.bind(orderCreateQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding stockDeductBinding(TopicExchange orderExchange, Queue stockDeductQueue) {
        return BindingBuilder.bind(stockDeductQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_CREATED);
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
    public Binding orderExpiredBinding(TopicExchange orderExchange, Queue orderExpiredQueue) {
        return BindingBuilder.bind(orderExpiredQueue).to(orderExchange).with(MqConstants.ROUTING_ORDER_EXPIRED);
    }

    /** 声明拓扑（应用启动时执行） */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
