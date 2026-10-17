-- 一次性迁移脚本：将 user_db 等库改名为 seck8s_xxx_db（保留数据，不删库）
-- 原理：建新库 + RENAME TABLE 搬表 + 删除空旧库
-- 若已执行过本脚本，再次执行 RENAME TABLE 会因表不存在而报错（可忽略）

-- ========== user_db → seck8s_user_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE user_db.users TO seck8s_user_db.users;
DROP DATABASE IF EXISTS user_db;

-- ========== ticket_db → seck8s_ticket_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_ticket_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE ticket_db.sku TO seck8s_ticket_db.sku,
             ticket_db.tickets TO seck8s_ticket_db.tickets;
DROP DATABASE IF EXISTS ticket_db;

-- ========== activity_db → seck8s_activity_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_activity_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE activity_db.activity TO seck8s_activity_db.activity,
             activity_db.activity_sku TO seck8s_activity_db.activity_sku;
DROP DATABASE IF EXISTS activity_db;

-- ========== inventory_db → seck8s_inventory_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_inventory_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE inventory_db.sku_stock TO seck8s_inventory_db.sku_stock,
             inventory_db.stock_flow_log TO seck8s_inventory_db.stock_flow_log;
DROP DATABASE IF EXISTS inventory_db;

-- ========== order_db → seck8s_order_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_order_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE order_db.orders TO seck8s_order_db.orders,
             order_db.tb_message_outbox TO seck8s_order_db.tb_message_outbox;
DROP DATABASE IF EXISTS order_db;

-- ========== payment_db → seck8s_payment_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_payment_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE payment_db.payment_record TO seck8s_payment_db.payment_record,
             payment_db.tb_message_outbox TO seck8s_payment_db.tb_message_outbox;
DROP DATABASE IF EXISTS payment_db;

-- ========== notification_db → seck8s_notification_db ==========
CREATE DATABASE IF NOT EXISTS seck8s_notification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
RENAME TABLE notification_db.notification TO seck8s_notification_db.notification;
DROP DATABASE IF EXISTS notification_db;
