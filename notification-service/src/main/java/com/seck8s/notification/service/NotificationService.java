package com.seck8s.notification.service;

import com.seck8s.notification.entity.Notification;

public interface NotificationService {

    /** 记录并发送通知（幂等：uk_notify type+orderId+userId 只一条） */
    void notify(Notification notification);
}
