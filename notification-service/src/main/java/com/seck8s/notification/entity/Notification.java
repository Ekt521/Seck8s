package com.seck8s.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String notifyId;
    private Long userId;
    private String orderId;
    private String type;        // ORDER_CREATED / ORDER_PAID / ORDER_CANCELLED
    private String channel;     // STATION / SMS / EMAIL
    private String content;
    private Integer notifyStatus;  // 0待发 1已发 2失败
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
