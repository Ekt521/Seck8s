package com.seck8s.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seck8s.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
