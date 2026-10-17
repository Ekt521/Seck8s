package com.seck8s.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表（发件箱）：与支付流水同库同事务写入，PAY_RESULT 事件可靠投递
 */
@Data
@TableName("tb_message_outbox")
public class MessageOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String bizType;
    private String bizKey;
    private String exchange;
    private String routingKey;
    private String payload;
    private Integer msgStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
