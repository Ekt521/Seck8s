-- activity_db：活动服务
USE seck8s_activity_db;

CREATE TABLE IF NOT EXISTS activity (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id VARCHAR(32)  NOT NULL COMMENT '活动全局业务ID',
    name        VARCHAR(128) NOT NULL COMMENT '活动名',
    start_time  DATETIME     NOT NULL COMMENT '开始时间',
    end_time    DATETIME     NOT NULL COMMENT '结束时间',
    activity_status TINYINT  DEFAULT 0 COMMENT '0草稿 1已发布 2进行中 3已结束',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_id (activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀活动/场次';

CREATE TABLE IF NOT EXISTS activity_sku (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id    VARCHAR(32)  NOT NULL COMMENT '活动ID',
    sku_id         VARCHAR(32)  NOT NULL COMMENT '参与秒杀的SKU',
    seckill_price  DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock_limit    INT          NOT NULL COMMENT '本场放量（用于库存预热）',
    quota_per_user INT          DEFAULT 1 COMMENT '每用户限购数量',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_act_sku (activity_id, sku_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='活动参与SKU';
