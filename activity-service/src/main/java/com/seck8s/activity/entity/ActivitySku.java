package com.seck8s.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity_sku")
public class ActivitySku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityId;
    private String skuId;
    private BigDecimal seckillPrice;
    private Integer stockLimit;      // 本场放量
    private Integer quotaPerUser;    // 每用户限购
    private LocalDateTime createTime;
}
