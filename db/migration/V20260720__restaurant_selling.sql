-- 餐厅售卖模块迁移脚本
-- 包含：菜单、订单、订单项

-- 1. 菜单表
CREATE TABLE IF NOT EXISTS restaurant_menus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pond_id BIGINT NOT NULL COMMENT '鱼塘ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    name VARCHAR(128) NOT NULL COMMENT '菜品名称',
    category VARCHAR(32) NOT NULL COMMENT '分类：fresh_fish-鲜鱼 cooked-加工菜品 drink-饮品',
    price INT NOT NULL DEFAULT 0 COMMENT '价格（分）',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存：-1 表示无限',
    image_url VARCHAR(512) DEFAULT NULL COMMENT '图片URL',
    description TEXT DEFAULT NULL COMMENT '描述',
    is_special TINYINT NOT NULL DEFAULT 0 COMMENT '是否招牌：0-否 1-是',
    status VARCHAR(16) NOT NULL DEFAULT 'on' COMMENT '状态：on-上架 off-下架',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pond_category (pond_id, category),
    INDEX idx_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐厅菜单表';

-- 2. 订单表（shop 模块复用）
CREATE TABLE IF NOT EXISTS shop_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    order_type VARCHAR(50) NOT NULL DEFAULT 'restaurant' COMMENT '订单类型：restaurant-餐厅订单',
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID',
    merchant_id BIGINT DEFAULT NULL COMMENT '商家ID',
    spot_id BIGINT DEFAULT NULL COMMENT '配送钓位ID',
    total_amount INT NOT NULL DEFAULT 0 COMMENT '订单总金额（分）',
    status VARCHAR(50) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理 cooking-制作中 delivered-已配送 completed-已完成 cancelled-已取消',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    INDEX idx_user (user_id),
    INDEX idx_pond (pond_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_order_type (order_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 兼容已存在的 shop_orders 表（由商城模块先创建时补齐餐厅所需字段）
ALTER TABLE shop_orders
    ADD COLUMN IF NOT EXISTS spot_id BIGINT DEFAULT NULL COMMENT '配送钓位ID',
    ADD COLUMN IF NOT EXISTS remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    ADD COLUMN IF NOT EXISTS paid_at DATETIME DEFAULT NULL COMMENT '支付时间';

-- 3. 订单项表（兼容商城与餐厅字段）
CREATE TABLE IF NOT EXISTS shop_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    menu_id BIGINT DEFAULT NULL COMMENT '菜单ID（餐厅订单）',
    menu_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称快照（餐厅订单）',
    price INT NOT NULL DEFAULT 0 COMMENT '单价（分）（餐厅订单）',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    product_id BIGINT DEFAULT NULL COMMENT '商品ID（商城订单）',
    product_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称（商城订单）',
    unit_price INT NOT NULL DEFAULT 0 COMMENT '下单时单价（分）（商城订单）',
    subtotal INT NOT NULL DEFAULT 0 COMMENT '小计金额（分）（商城订单）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 兼容已存在的 shop_order_items 表（由商城模块先创建时补齐餐厅所需字段）
ALTER TABLE shop_order_items
    ADD COLUMN IF NOT EXISTS menu_id BIGINT DEFAULT NULL COMMENT '菜单ID（餐厅订单）',
    ADD COLUMN IF NOT EXISTS menu_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称快照（餐厅订单）',
    ADD COLUMN IF NOT EXISTS price INT NOT NULL DEFAULT 0 COMMENT '单价（分）（餐厅订单）',
    MODIFY COLUMN product_id BIGINT DEFAULT NULL COMMENT '商品ID（商城订单）',
    MODIFY COLUMN product_name VARCHAR(128) DEFAULT NULL COMMENT '商品名称（商城订单）';
