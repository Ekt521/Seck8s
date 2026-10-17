package com.seck8s.payment.scheduler;

import com.seck8s.payment.service.PaymentOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付发件箱定时发布器
 */
@Slf4j
@Component
@EnableScheduling
public class OutboxPublisher {

    private final PaymentOutboxService outboxService;

    @Value("${seck8s.outbox.scan-batch:100}")
    private int batchSize;

    public OutboxPublisher(PaymentOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        try {
            int count = outboxService.publishPending(batchSize);
            if (count > 0) {
                log.info("支付发件箱本轮投递 {} 条", count);
            }
        } catch (Exception e) {
            log.error("支付发件箱定时发布异常", e);
        }
    }
}
