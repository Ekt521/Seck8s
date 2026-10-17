package com.seck8s.order.scheduler;

import com.seck8s.order.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地消息表定时发布器：每秒扫描 status=0 待发消息，投递 MQ（confirm 确认后置已发）
 */
@Slf4j
@Component
@EnableScheduling
public class OutboxPublisher {

    private final OutboxService outboxService;

    @Value("${seck8s.outbox.scan-batch:100}")
    private int batchSize;

    public OutboxPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        try {
            int count = outboxService.publishPending(batchSize);
            if (count > 0) {
                log.info("发件箱本轮投递 {} 条", count);
            }
        } catch (Exception e) {
            log.error("发件箱定时发布异常", e);
        }
    }
}
