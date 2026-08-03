-- 商城/餐饮模块迁移脚本

-- 1. 商品表
CREATE TABLE IF NOT EXISTS shop_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID（可选，用于关联特定鱼塘）',
    merchant_id BIGINT NOT NULL COMMENT '所属商家ID',
    category VARCHAR(50) NOT NULL COMMENT '分类：equipment-钓具/bait-饵料/fish-鱼获/food-菜品',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price INT NOT NULL DEFAULT 0 COMMENT '单价（分）',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    image_url VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
    description TEXT DEFAULT NULL COMMENT '商品描述',
    status VARCHAR(20) NOT NULL DEFAULT 'on' COMMENT '状态：on-上架/off-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_pond_id (pond_id),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城商品表';

-- 2. 订单表
CREATE TABLE IF NOT EXISTS shop_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID（可选）',
    order_type VARCHAR(20) NOT NULL DEFAULT 'shop' COMMENT '订单类型：shop-商城/restaurant-餐饮',
    total_amount INT NOT NULL DEFAULT 0 COMMENT '订单总金额（分）',
    status VARCHAR(20) NOT NULL DEFAULT 'pending_pay' COMMENT '状态：pending_pay-待支付/paid-已支付/completed-已完成/cancelled-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_pond_id (pond_id),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单表';

-- 3. 订单明细表
CREATE TABLE IF NOT EXISTS shop_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price INT NOT NULL DEFAULT 0 COMMENT '下单时单价（分）',
    subtotal INT NOT NULL DEFAULT 0 COMMENT '小计金额（分）',
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单明细表';
