package com.seck8s.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_flow_log")
public class StockFlowLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String skuId;
    private String orderId;
    private String changeType;   // INIT / DEDUCT / RESTORE
    private Integer changeQty;   // 扣减为负，回补为正
    private Integer beforeStock;
    private Integer afterStock;
    private LocalDateTime createTime;
}
