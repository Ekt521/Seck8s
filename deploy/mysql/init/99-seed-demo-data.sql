-- 演示种子数据（可执行可不执行；用户走注册接口创建，不做密码种子）
USE seck8s_ticket_db;
INSERT INTO tickets (ticket_id, name, description, category, price, ticket_status)
VALUES ('T20260804001', '《盛夏演唱会》', '夏日限定演唱会，全城首演', 'CONCERT', 588.00, 1);
INSERT INTO sku (sku_id, ticket_id, name, price, sku_status) VALUES
    ('SKU001', 'T20260804001', 'VIP区座', 888.00, 1),
    ('SKU002', 'T20260804001', '普通区座', 388.00, 1);

USE seck8s_activity_db;
INSERT INTO activity (activity_id, name, start_time, end_time, activity_status)
VALUES ('ACT20260804001', '《盛夏演唱会》首轮秒杀', '2026-08-10 20:00:00', '2026-08-10 21:00:00', 1);
INSERT INTO activity_sku (activity_id, sku_id, seckill_price, stock_limit, quota_per_user) VALUES
    ('ACT20260804001', 'SKU001', 399.00, 100, 1),
    ('ACT20260804001', 'SKU002', 199.00, 200, 1);

USE seck8s_inventory_db;
INSERT INTO sku_stock (sku_id, stock, version) VALUES
    ('SKU001', 100, 0),
    ('SKU002', 200, 0);
INSERT INTO stock_flow_log (sku_id, order_id, change_type, change_qty, before_stock, after_stock) VALUES
    ('SKU001', NULL, 'INIT', 100, 0, 100),
    ('SKU002', NULL, 'INIT', 200, 0, 200);
