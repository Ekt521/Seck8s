-- order_db：订单服务
USE seck8s_order_db;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      VARCHAR(64)   NOT NULL COMMENT '全局唯一订单号（Snowflake）',
    user_id       BIGINT        NOT NULL COMMENT '下单用户',
    activity_id   VARCHAR(32)   NOT NULL COMMENT '秒杀活动ID',
    sku_id        VARCHAR(32)   NOT NULL COMMENT '秒杀SKU',
    ticket_id     VARCHAR(32)   DEFAULT NULL COMMENT '所属票（冗余便于展示）',
    quantity      INT           DEFAULT 1 COMMENT '购买数量',
    amount        DECIMAL(10,2) NOT NULL COMMENT '订单金额 = 秒杀价*数量',
    order_status  VARCHAR(16)   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID待支付/PAID已支付/CANCELLED已取消',
    expire_time   DATETIME      NOT NULL COMMENT '支付截止时间 = 创建 + TTL',
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_id (order_id),            -- 幂等建单的关键
    KEY idx_user (user_id),
    KEY idx_status_expire (order_status, expire_time) -- 定时扫单兜底
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE IF NOT EXISTS tb_message_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id          VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务类型：ORDER_CREATE/ORDER_CANCEL/ORDER_PAID/PAY_RESULT',
    biz_key         VARCHAR(64) NOT NULL COMMENT '业务键（如 order_id）',
    exchange        VARCHAR(64) NOT NULL COMMENT '目标交换机',
    routing_key     VARCHAR(64) NOT NULL COMMENT '路由键',
    payload         TEXT        NOT NULL COMMENT '消息体JSON',
    msg_status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0待发 1已发 2重试超限',
    retry_count     INT         DEFAULT 0 COMMENT '已重试次数',
    next_retry_time DATETIME    DEFAULT NULL COMMENT '下次重试时间（指数退避）',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_status_time (msg_status, next_retry_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='本地消息表（发件箱）';
