package com.seck8s.payment.config;

import com.seck8s.common.constant.MqConstants;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付服务 RabbitMQ 拓扑：声明支付交换机（pay.result 队列由 order/notification 各自声明）
 */
@Configuration
public class RabbitMqConfig {

    /** 支付事件交换机 */
    @Bean
    public TopicExchange payExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.PAY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
