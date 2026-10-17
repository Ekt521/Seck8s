package com.seck8s.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tickets")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketId;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer ticketStatus;   // 0下架 1上架
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
