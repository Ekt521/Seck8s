package com.seck8s.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表（发件箱）：与业务变更同库同事务写入，定时扫描投递 MQ，保证可靠投递
 */
@Data
@TableName("tb_message_outbox")
public class MessageOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String bizType;       // ORDER_CREATE / ORDER_CANCEL / ORDER_PAID
    private String bizKey;        // order_id
    private String exchange;
    private String routingKey;
    private String payload;       // 消息体 JSON
    private Integer msgStatus;    // 0待发 1已发 2重试超限
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
