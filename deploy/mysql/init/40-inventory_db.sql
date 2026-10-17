-- inventory_db：库存服务（库存唯一真相源）
USE seck8s_inventory_db;

CREATE TABLE IF NOT EXISTS sku_stock (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id      VARCHAR(32) NOT NULL COMMENT 'SKU业务ID',
    stock       INT         NOT NULL DEFAULT 0 COMMENT '剩余库存',
    version     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SKU库存';

CREATE TABLE IF NOT EXISTS stock_flow_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id       VARCHAR(32) NOT NULL COMMENT 'SKU业务ID',
    order_id     VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
    change_type  VARCHAR(16) NOT NULL COMMENT 'INIT初始化/DEDUCT扣减/RESTORE回补',
    change_qty   INT         NOT NULL COMMENT '变化数量（扣减为负，回补为正）',
    before_stock INT         NOT NULL COMMENT '变化前库存',
    after_stock  INT         NOT NULL COMMENT '变化后库存',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sku (sku_id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='库存流水（对账/审计）';
