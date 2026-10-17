package com.seck8s.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sku")
public class Sku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String skuId;
    private String ticketId;
    private String name;
    private BigDecimal price;
    private Integer skuStatus;   // 0下架 1上架
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 实时库存余量（非表字段，查票接口实时填充） */
    @TableField(exist = false)
    private Integer extStock;
}
