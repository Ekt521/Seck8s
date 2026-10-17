-- ticket_db：票务/商品服务（票 + SKU 主数据；库存不在本库，在 inventory_db）
USE seck8s_ticket_db;

CREATE TABLE IF NOT EXISTS tickets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id   VARCHAR(32)  NOT NULL COMMENT '票全局业务ID',
    name        VARCHAR(128) NOT NULL COMMENT '票名，如《XXX演唱会》',
    description VARCHAR(512) DEFAULT NULL COMMENT '描述',
    category    VARCHAR(32)  DEFAULT NULL COMMENT '分类：CONCERT/DRAMA/SPORT...',
    price       DECIMAL(10,2) NOT NULL COMMENT '原价',
    ticket_status TINYINT    DEFAULT 1 COMMENT '0下架 1上架',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_id (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='票主数据';

CREATE TABLE IF NOT EXISTS sku (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id      VARCHAR(32)  NOT NULL COMMENT 'SKU全局业务ID，跨库join key',
    ticket_id   VARCHAR(32)  NOT NULL COMMENT '所属票',
    name        VARCHAR(64)  NOT NULL COMMENT 'SKU名，如 VIP区座/普通区座',
    price       DECIMAL(10,2) NOT NULL COMMENT 'SKU原价',
    sku_status  TINYINT      DEFAULT 1 COMMENT '0下架 1上架',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_id (sku_id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='票SKU';
