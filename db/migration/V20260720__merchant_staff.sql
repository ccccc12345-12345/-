-- 商家员工账号管理迁移脚本

-- 1. 用户表新增员工关联字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS staff_id BIGINT DEFAULT NULL COMMENT '关联员工表ID';

-- 2. 创建商家员工表
CREATE TABLE IF NOT EXISTS merchant_staff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    merchant_id BIGINT NOT NULL COMMENT '所属商家ID',
    user_id BIGINT NOT NULL COMMENT '关联系统用户ID',
    staff_name VARCHAR(100) NOT NULL COMMENT '员工姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色：checker-核销员 operator-运营 finance-财务 manager-店长',
    status VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态：normal-正常 disabled-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家员工表';
