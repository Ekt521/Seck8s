-- payment_db：支付服务
USE seck8s_payment_db;

CREATE TABLE IF NOT EXISTS payment_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id  VARCHAR(64)   NOT NULL COMMENT '支付流水号',
    order_id    VARCHAR(64)   NOT NULL COMMENT '订单号',
    user_id     BIGINT        NOT NULL COMMENT '用户',
    amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    channel     VARCHAR(16)   DEFAULT 'SIMULATE' COMMENT '支付渠道：SIMULATE模拟/WX/ALIPAY（可插拔）',
    pay_status  VARCHAR(16)   DEFAULT 'CREATED' COMMENT 'CREATED待支付/SUCCESS成功/FAILED失败/CLOSED关闭',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_id (payment_id),
    UNIQUE KEY uk_order (order_id)  -- 一单一支付，天然幂等
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付流水';

CREATE TABLE IF NOT EXISTS tb_message_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id          VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型：PAY_RESULT',
    biz_key         VARCHAR(64) NOT NULL COMMENT '业务键（order_id）',
    exchange        VARCHAR(64) NOT NULL COMMENT '目标交换机',
    routing_key     VARCHAR(64) NOT NULL COMMENT '路由键',
    payload         TEXT        NOT NULL COMMENT '消息体JSON',
    msg_status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0待发 1已发 2重试超限',
    retry_count     INT         DEFAULT 0 COMMENT '已重试次数',
    next_retry_time DATETIME    DEFAULT NULL COMMENT '下次重试时间',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_status_time (msg_status, next_retry_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='本地消息表（发件箱）';
