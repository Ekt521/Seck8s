-- user_db：用户服务
USE seck8s_user_db;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    id_card     VARCHAR(30)  DEFAULT NULL COMMENT '身份证号',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';
