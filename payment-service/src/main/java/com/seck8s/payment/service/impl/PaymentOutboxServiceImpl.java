package com.seck8s.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seck8s.payment.entity.MessageOutbox;
import com.seck8s.payment.mapper.MessageOutboxMapper;
import com.seck8s.payment.service.PaymentOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付服务发件箱：定时扫描待发消息投递 MQ（PAY_RESULT 事件）
 */
@Slf4j
@Service
public class PaymentOutboxServiceImpl implements PaymentOutboxService {

    private final MessageOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${seck8s.outbox.max-retry:10}")
    private int maxRetry;

    public PaymentOutboxServiceImpl(MessageOutboxMapper outboxMapper, RabbitTemplate rabbitTemplate) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public void save(MessageOutbox message) {
        message.setMsgStatus(0);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        outboxMapper.insert(message);
    }

    @Override
    public int publishPending(int batchSize) {
        List<MessageOutbox> pending = outboxMapper.selectList(new LambdaQueryWrapper<MessageOutbox>()
                .eq(MessageOutbox::getMsgStatus, 0)
                .le(MessageOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(MessageOutbox::getId)
                .last("limit " + batchSize));
        for (MessageOutbox msg : pending) {
            publishOne(msg);
        }
        return pending.size();
    }

    private void publishOne(MessageOutbox msg) {
        try {
            rabbitTemplate.convertAndSend(msg.getExchange(), msg.getRoutingKey(), msg.getPayload());
            outboxMapper.update(null, new LambdaUpdateWrapper<MessageOutbox>()
                    .eq(MessageOutbox::getId, msg.getId())
                    .set(MessageOutbox::getMsgStatus, 1));
            log.info("支付发件箱投递成功 msgId={} orderId={}", msg.getMsgId(), msg.getBizKey());
        } catch (Exception e) {
            log.error("支付发件箱投递失败 msgId={}: {}", msg.getMsgId(), e.getMessage());
            int retry = msg.getRetryCount() + 1;
            int status = retry >= maxRetry ? 2 : 0;
            outboxMapper.update(null, new LambdaUpdateWrapper<MessageOutbox>()
                    .eq(MessageOutbox::getId, msg.getId())
                    .set(MessageOutbox::getRetryCount, retry)
                    .set(MessageOutbox::getMsgStatus, status)
                    .set(MessageOutbox::getNextRetryTime,
                            LocalDateTime.now().plusSeconds(Math.min(60L << Math.min(retry, 6), 3600L))));
            if (status == 2) {
                log.error("支付发件箱消息重试超限 msgId={}", msg.getMsgId());
            }
        }
    }
}
