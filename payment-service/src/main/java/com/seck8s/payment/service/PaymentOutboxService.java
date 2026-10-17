package com.seck8s.payment.service;

import com.seck8s.payment.entity.MessageOutbox;

public interface PaymentOutboxService {

    /** 事务内写入发件箱 */
    void save(MessageOutbox message);

    /** 定时扫描待发消息并投递 MQ */
    int publishPending(int batchSize);
}
