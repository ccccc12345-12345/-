CREATE DATABASE IF NOT EXISTS fishing_reservation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fishing_reservation;

DROP TABLE IF EXISTS draw_result;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS fishing_spot;
DROP TABLE IF EXISTS time_slot;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE time_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    slot_date DATE NOT NULL COMMENT '日期',
    slot_name VARCHAR(50) NOT NULL COMMENT '场次名称：早场/午场/晚场/全天场',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    max_bookings INT NOT NULL DEFAULT 0 COMMENT '最大预约人数',
    advance_days INT NOT NULL DEFAULT 7 COMMENT '提前N天可约',
    draw_start_time DATETIME NOT NULL COMMENT '抽号开始时间',
    draw_end_time DATETIME NOT NULL COMMENT '抽号结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-禁用 1-启用',
    INDEX idx_slot_date (slot_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时段配置';

CREATE TABLE fishing_spot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    spot_code VARCHAR(20) NOT NULL UNIQUE COMMENT '钓位编号，如 A01',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-维修/禁用 1-可用',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钓位表';

CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    slot_id BIGINT NOT NULL COMMENT '时段ID',
    status VARCHAR(20) NOT NULL DEFAULT '待抽号' COMMENT '状态：待抽号/已抽号/预约取消/过期失效',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    cancel_time DATETIME COMMENT '取消时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    UNIQUE KEY uk_user_slot (user_id, slot_id),
    INDEX idx_slot_id (slot_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录';

CREATE TABLE draw_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    reservation_id BIGINT NOT NULL UNIQUE COMMENT '预约ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    slot_id BIGINT NOT NULL COMMENT '时段ID',
    spot_id BIGINT NOT NULL COMMENT '钓位ID',
    draw_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抽号时间',
    UNIQUE KEY uk_slot_spot (slot_id, spot_id),
    INDEX idx_user_id (user_id),
    INDEX idx_slot_id (slot_id),
    INDEX idx_spot_id (spot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽号结果';

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    password VARCHAR(120) NOT NULL COMMENT '加密密码',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通用户 1-管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT INTO fishing_spot (spot_code, status) VALUES
('A01', 1), ('A02', 1), ('A03', 1), ('A04', 1), ('A05', 1),
('B01', 1), ('B02', 1), ('B03', 1), ('B04', 1), ('B05', 1),
('C01', 1), ('C02', 1), ('C03', 1), ('C04', 1), ('C05', 1);

INSERT INTO sys_user (phone, nickname, password, role, status) VALUES
('admin', '管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', 1, 1);
