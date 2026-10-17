package com.seck8s.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sku_stock")
public class SkuStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String skuId;
    private Integer stock;
    private Integer version;   // 乐观锁版本号
    private LocalDateTime updateTime;
}
