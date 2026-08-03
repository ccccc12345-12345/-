-- 渔获记录与回收模块迁移脚本
CREATE TABLE IF NOT EXISTS catch_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pond_id BIGINT NOT NULL COMMENT '鱼塘ID',
    reservation_id BIGINT DEFAULT NULL COMMENT '预约ID',
    spot_id BIGINT DEFAULT NULL COMMENT '钓位ID',
    fish_type VARCHAR(100) NOT NULL COMMENT '鱼种',
    weight DECIMAL(10, 3) NOT NULL DEFAULT 0 COMMENT '重量（千克）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '照片URL',
    status VARCHAR(50) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理/sold_recycle-已回收/sold_restaurant-已售餐厅/released-已放生',
    recycle_price INT DEFAULT NULL COMMENT '回收价格（分）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_pond_id (pond_id),
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渔获记录表';
