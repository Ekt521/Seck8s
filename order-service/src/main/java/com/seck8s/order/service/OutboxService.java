package com.seck8s.order.service;

import com.seck8s.order.entity.MessageOutbox;

public interface OutboxService {

    /**
     * 事务内写入发件箱（与业务变更同库同事务），立即置 status=0 待发
     */
    void save(MessageOutbox message);

    /** 定时扫描待发消息并投递 MQ（OutboxPublisher 调用） */
    int publishPending(int batchSize);
}
