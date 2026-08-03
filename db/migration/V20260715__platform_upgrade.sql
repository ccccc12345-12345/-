-- 钓鱼场预约系统平台化升级迁移脚本
-- 包含：多鱼塘、商家、核销、收益、分享、角色扩展

-- 1. 用户表角色扩展
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS role TINYINT DEFAULT 0 COMMENT '角色：0-普通用户 1-商家 2-平台管理员';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS admin_type TINYINT DEFAULT 0 COMMENT '管理员类型：0-超级管理员 1-普通管理员';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '绑定鱼塘ID（普通管理员生效）';

-- 2. 创建鱼塘表
CREATE TABLE IF NOT EXISTS pond (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT DEFAULT NULL COMMENT '所属商家ID',
    name VARCHAR(100) NOT NULL COMMENT '鱼塘名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鱼塘表';

-- 3. 给现有业务表添加鱼塘ID字段
ALTER TABLE time_slot ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE fishing_spot ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE draw_result ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';

-- 4. 给预约表添加核销和收益字段
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS checkin_code VARCHAR(20) DEFAULT NULL COMMENT '6位数字核销码';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS actual_fee DECIMAL(10,2) DEFAULT NULL COMMENT '实际收费金额';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS check_in_time DATETIME DEFAULT NULL COMMENT '核销时间';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消/作废原因';

-- 5. 给时段表添加默认票价
ALTER TABLE time_slot ADD COLUMN IF NOT EXISTS default_price DECIMAL(10,2) DEFAULT NULL COMMENT '默认票价';

-- 5.1 给用户表添加邮箱和最后登录时间
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS email VARCHAR(100) DEFAULT NULL COMMENT '邮箱';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间';

-- 6. 创建邀请码表
CREATE TABLE IF NOT EXISTS invite_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '邀请码',
    merchant_id BIGINT DEFAULT NULL COMMENT '绑定商家ID',
    used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用：0未使用 1已使用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表';

-- 7. 创建分享令牌表
CREATE TABLE IF NOT EXISTS share_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(64) NOT NULL UNIQUE COMMENT '分享令牌',
    pond_id BIGINT NOT NULL COMMENT '鱼塘ID',
    slot_id BIGINT NOT NULL COMMENT '时段ID',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享令牌表';

-- 8. 旧角色迁移：role=1 旧管理员 -> 新平台管理员 role=2
UPDATE sys_user SET role = 2 WHERE role = 1;

-- 9. 初始化默认商家账号（用于兼容旧数据）
-- 注意：实际密码应使用 BCrypt 加密，此处仅作为示例占位
-- INSERT INTO sys_user (phone, nickname, password, role, status, create_time)
-- VALUES ('merchant', '默认商家', '$2a$10$...', 1, 1, NOW());

-- 10. 初始化默认邀请码（测试环境）
-- INSERT INTO invite_code (code, merchant_id, used) VALUES ('FISH2024', 1, 0);

-- 11. 可选：外键约束（生产环境按需开启）
-- ALTER TABLE time_slot ADD CONSTRAINT fk_time_slot_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE fishing_spot ADD CONSTRAINT fk_fishing_spot_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE reservation ADD CONSTRAINT fk_reservation_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE draw_result ADD CONSTRAINT fk_draw_result_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
