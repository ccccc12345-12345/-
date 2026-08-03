-- 多鱼塘、核销、收益管理迁移脚本

-- 1. 创建鱼塘表
CREATE TABLE IF NOT EXISTS pond (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '鱼塘名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鱼塘表';

-- 2. 给现有表添加鱼塘ID字段
ALTER TABLE time_slot ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE fishing_spot ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';
ALTER TABLE draw_result ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID';

-- 3. 给预约表添加核销和收益字段
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS checkin_code VARCHAR(20) DEFAULT NULL COMMENT '核销码';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS actual_fee DECIMAL(10,2) DEFAULT NULL COMMENT '实际收费金额';
ALTER TABLE reservation ADD COLUMN IF NOT EXISTS check_in_time DATETIME DEFAULT NULL COMMENT '核销时间';

-- 4. 给时段表添加默认票价
ALTER TABLE time_slot ADD COLUMN IF NOT EXISTS default_price DECIMAL(10,2) DEFAULT NULL COMMENT '默认票价';

-- 5. 给用户表添加管理员类型和绑定鱼塘
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS admin_type TINYINT DEFAULT 0 COMMENT '管理员类型：0-超级管理员 1-普通管理员';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS pond_id BIGINT DEFAULT NULL COMMENT '绑定鱼塘ID（普通管理员生效）';

-- 6. 创建默认鱼塘
INSERT INTO pond (name, address, phone, status) VALUES ('默认鱼塘', '系统默认鱼塘', '', 1);

-- 7. 将现有数据归属到默认鱼塘
SET @default_pond_id = LAST_INSERT_ID();

UPDATE time_slot SET pond_id = @default_pond_id WHERE pond_id IS NULL;
UPDATE fishing_spot SET pond_id = @default_pond_id WHERE pond_id IS NULL;
UPDATE reservation SET pond_id = @default_pond_id WHERE pond_id IS NULL;
UPDATE draw_result SET pond_id = @default_pond_id WHERE pond_id IS NULL;

-- 7. 添加外键约束（可选，先保留不强制，便于后续数据维护）
-- ALTER TABLE time_slot ADD CONSTRAINT fk_time_slot_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE fishing_spot ADD CONSTRAINT fk_fishing_spot_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE reservation ADD CONSTRAINT fk_reservation_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
-- ALTER TABLE draw_result ADD CONSTRAINT fk_draw_result_pond FOREIGN KEY (pond_id) REFERENCES pond(id);
