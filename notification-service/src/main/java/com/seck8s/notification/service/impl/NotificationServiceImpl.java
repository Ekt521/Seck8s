package com.seck8s.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seck8s.notification.entity.Notification;
import com.seck8s.notification.mapper.NotificationMapper;
import com.seck8s.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务：幂等落库 + 模拟发送（站内信/短信/邮件），失败置 2 待重试
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Override
    public void notify(Notification notification) {
        // 幂等：同订单同类型同用户只记录一条
        Notification exist = this.getOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getType, notification.getType())
                .eq(Notification::getOrderId, notification.getOrderId())
                .eq(Notification::getUserId, notification.getUserId()));
        if (exist != null) {
            log.info("通知已存在，跳过 type={} orderId={} userId={}",
                    notification.getType(), notification.getOrderId(), notification.getUserId());
            return;
        }
        notification.setNotifyStatus(0);
        notification.setRetryCount(0);
        this.save(notification);
        // 模拟发送
        send(notification);
    }

    private void send(Notification notification) {
        try {
            Thread.sleep(50); // 模拟外部渠道耗时
            notification.setNotifyStatus(1);
            this.updateById(notification);
            log.info("[通知发送成功] type={} userId={} orderId={} channel={} content={}",
                    notification.getType(), notification.getUserId(),
                    notification.getOrderId(), notification.getChannel(), notification.getContent());
        } catch (Exception e) {
            log.error("[通知发送失败] type={} orderId={}", notification.getType(), notification.getOrderId(), e);
            notification.setNotifyStatus(2);
            this.updateById(notification);
        }
    }
}
