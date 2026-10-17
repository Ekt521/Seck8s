package com.seck8s.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seck8s.order.entity.MessageOutbox;
import com.seck8s.order.mapper.MessageOutboxMapper;
import com.seck8s.order.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表（发件箱）实现：定时扫描待发消息 → RabbitMQ confirm 投递 → 置已发
 */
@Slf4j
@Service
public class OutboxServiceImpl implements OutboxService {

    private final MessageOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${seck8s.outbox.max-retry:10}")
    private int maxRetry;

    public OutboxServiceImpl(MessageOutboxMapper outboxMapper, RabbitTemplate rabbitTemplate) {
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
            log.info("发件箱投递成功 msgId={} bizType={} orderId={}", msg.getMsgId(), msg.getBizType(), msg.getBizKey());
        } catch (Exception e) {
            log.error("发件箱投递失败 msgId={}: {}", msg.getMsgId(), e.getMessage());
            int retry = msg.getRetryCount() + 1;
            int status = retry >= maxRetry ? 2 : 0;   // 超限置 2 待人工介入
            outboxMapper.update(null, new LambdaUpdateWrapper<MessageOutbox>()
                    .eq(MessageOutbox::getId, msg.getId())
                    .set(MessageOutbox::getRetryCount, retry)
                    .set(MessageOutbox::getMsgStatus, status)
                    .set(MessageOutbox::getNextRetryTime,
                            LocalDateTime.now().plusSeconds(Math.min(60L << Math.min(retry, 6), 3600L))));
            if (status == 2) {
                log.error("发件箱消息重试超限，需人工介入 msgId={}", msg.getMsgId());
            }
        }
    }
}
