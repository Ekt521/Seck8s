package com.seck8s.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;        // 全局唯一订单号（Snowflake）
    private Long userId;
    private String activityId;
    private String skuId;
    private String ticketId;       // 冗余便于展示
    private Integer quantity;
    private BigDecimal amount;     // 秒杀价*数量
    private String orderStatus;    // UNPAID / PAID / CANCELLED
    private LocalDateTime expireTime; // 支付截止时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
