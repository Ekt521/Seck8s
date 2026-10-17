package com.seck8s.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法订单号生成器（全局唯一）。
 * 各服务实例通过 seck8s.snowflake.{datacenter-id,worker-id} 区分，默认 0。
 */
@Component
public class OrderNoGenerator {

    private static final long EPOCH = 1735689600000L; // 2025-01-01 00:00:00 UTC

    private final long datacenterId;
    private final long workerId;
    private final long datacenterIdBits = 5L;
    private final long workerIdBits = 5L;
    private final long sequenceBits = 12L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public OrderNoGenerator(@Value("${seck8s.snowflake.datacenter-id:0}") long datacenterId,
                            @Value("${seck8s.snowflake.worker-id:0}") long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 超范围");
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId 超范围");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("时钟回拨，拒绝生成ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    public String nextOrderId() {
        return String.valueOf(nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
