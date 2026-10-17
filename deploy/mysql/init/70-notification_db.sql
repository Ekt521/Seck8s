-- notification_db：通知服务
USE seck8s_notification_db;

CREATE TABLE IF NOT EXISTS notification (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    notify_id    VARCHAR(64)  NOT NULL COMMENT '通知唯一ID',
    user_id      BIGINT       NOT NULL COMMENT '接收用户',
    order_id     VARCHAR(64)  DEFAULT NULL COMMENT '关联订单',
    type         VARCHAR(24)  NOT NULL COMMENT '通知类型：ORDER_CREATED/ORDER_PAID/ORDER_CANCELLED',
    channel      VARCHAR(16)  DEFAULT 'STATION' COMMENT '渠道：STATION站内/SMS短信/EMAIL',
    content      VARCHAR(512) DEFAULT NULL COMMENT '内容',
    notify_status TINYINT     DEFAULT 0 COMMENT '0待发 1已发 2失败',
    retry_count  INT          DEFAULT 0 COMMENT '重试次数',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notify (type, order_id, user_id)  -- 幂等：同订单同类型同用户只一条
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知记录';
