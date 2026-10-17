package com.seck8s.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentId;
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String channel;    // SIMULATE / WX / ALIPAY
    private String payStatus;  // CREATED / SUCCESS / FAILED / CLOSED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
