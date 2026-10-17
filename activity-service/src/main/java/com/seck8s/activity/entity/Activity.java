package com.seck8s.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityId;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer activityStatus;  // 0草稿 1已发布 2进行中 3已结束
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
