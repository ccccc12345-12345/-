package com.example.fishing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库结构自动迁移
 * 启动时自动创建新表、添加新字段、初始化默认鱼塘
 */
@Slf4j
@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            createPondTable(conn);
            addColumnIfNotExists(conn, "time_slot", "pond_id", "BIGINT");
            addColumnIfNotExists(conn, "fishing_spot", "pond_id", "BIGINT");
            addColumnIfNotExists(conn, "reservation", "pond_id", "BIGINT");
            addColumnIfNotExists(conn, "draw_result", "pond_id", "BIGINT");
            addColumnIfNotExists(conn, "reservation", "checkin_code", "VARCHAR(20)");
            addColumnIfNotExists(conn, "reservation", "actual_fee", "DECIMAL(10,2)");
            addColumnIfNotExists(conn, "reservation", "check_in_time", "DATETIME");
            addColumnIfNotExists(conn, "reservation", "cancel_reason", "VARCHAR(255)");
            addColumnIfNotExists(conn, "time_slot", "default_price", "DECIMAL(10,2)");
            addColumnIfNotExists(conn, "sys_user", "admin_type", "TINYINT");
            addColumnIfNotExists(conn, "sys_user", "pond_id", "BIGINT");
            addColumnIfNotExists(conn, "sys_user", "email", "VARCHAR(100)");
            addColumnIfNotExists(conn, "sys_user", "last_login_time", "DATETIME");
            addColumnIfNotExists(conn, "sys_user", "staff_id", "BIGINT");
            addColumnIfNotExists(conn, "pond", "merchant_id", "BIGINT");
            createInviteCodeTable(conn);
            createShareTokenTable(conn);
            createMerchantStaffTable(conn);
            migrateRoles(conn);
            Long merchantId = initDefaultMerchant(conn);
            initDefaultPond(conn, merchantId);
            initDefaultInviteCode(conn, merchantId);
            createShopTables(conn);
            createCatchRecordTable(conn);
            createRestaurantMenuTable(conn);
            log.info("数据库结构自动迁移完成");
        }
    }

    private void createPondTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS pond (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(100) NOT NULL COMMENT '鱼塘名称'," +
                "address VARCHAR(255) DEFAULT NULL COMMENT '地址'," +
                "phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话'," +
                "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用'," +
                "deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鱼塘表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("pond 表检查/创建完成");
        }
    }

    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType) throws Exception {
        String checkSql = "SELECT 1 FROM information_schema.COLUMNS " +
                "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (!rs.next()) {
                String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + " DEFAULT NULL";
                stmt.execute(alterSql);
                log.info("表 {} 添加字段 {} 完成", tableName, columnName);
            }
        }
    }

    private void initDefaultPond(Connection conn, Long merchantId) throws Exception {
        Long pondId;
        String querySql = "SELECT id FROM pond WHERE name = '默认鱼塘' AND deleted = 0 LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {
            if (rs.next()) {
                pondId = rs.getLong("id");
            } else {
                String insertSql = "INSERT INTO pond (name, address, phone, status, merchant_id) VALUES ('默认鱼塘', '系统默认鱼塘', '', 1, " + merchantId + ")";
                stmt.executeUpdate(insertSql, Statement.RETURN_GENERATED_KEYS);
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    pondId = keys.getLong(1);
                }
                log.info("创建默认鱼塘，ID: {}", pondId);
            }
        }

        // 将现有数据归属到默认鱼塘，并修正无商家的鱼塘
        String[] tables = {"time_slot", "fishing_spot", "reservation", "draw_result"};
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE pond SET merchant_id = " + merchantId + " WHERE merchant_id IS NULL");
            for (String table : tables) {
                stmt.executeUpdate("UPDATE " + table + " SET pond_id = " + pondId + " WHERE pond_id IS NULL");
            }
            log.info("现有数据已归属到默认鱼塘");
        }
    }

    private void createInviteCodeTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS invite_code (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "code VARCHAR(50) NOT NULL UNIQUE COMMENT '邀请码'," +
                "merchant_id BIGINT DEFAULT NULL COMMENT '绑定商家ID'," +
                "used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用：0未使用 1已使用'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("invite_code 表检查/创建完成");
        }
    }

    private void createShareTokenTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS share_token (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "token VARCHAR(64) NOT NULL UNIQUE COMMENT '分享令牌'," +
                "pond_id BIGINT NOT NULL COMMENT '鱼塘ID'," +
                "slot_id BIGINT NOT NULL COMMENT '时段ID'," +
                "expire_time DATETIME NOT NULL COMMENT '过期时间'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享令牌表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("share_token 表检查/创建完成");
        }
    }

    private void createMerchantStaffTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS merchant_staff (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID'," +
                "merchant_id BIGINT NOT NULL COMMENT '所属商家ID'," +
                "user_id BIGINT NOT NULL COMMENT '关联系统用户ID'," +
                "staff_name VARCHAR(100) NOT NULL COMMENT '员工姓名'," +
                "phone VARCHAR(20) NOT NULL COMMENT '手机号'," +
                "role VARCHAR(20) NOT NULL COMMENT '角色：checker-核销员 operator-运营 finance-财务 manager-店长'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态：normal-正常 disabled-禁用'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_phone (phone)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家员工表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("merchant_staff 表检查/创建完成");
        }
    }

    private void migrateRoles(Connection conn) throws Exception {
        // 旧系统 role=1 表示管理员，新系统 role=1 表示商家、role=2 表示平台管理员
        // 只迁移已知的旧管理员账号，避免把商家角色误改为管理员
        try (Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate("UPDATE sys_user SET role = 2 WHERE role = 1 AND phone = 'admin'");
            if (rows > 0) {
                log.info("旧管理员角色已迁移为平台管理员(role=2)，数量: {}", rows);
            }
        }
    }

    private Long initDefaultMerchant(Connection conn) throws Exception {
        // 修正默认商家及演示老板账号的角色，确保不会被角色迁移误伤
        String[] merchantPhones = {"merchant", "18800000001"};
        for (String phone : merchantPhones) {
            String querySql = "SELECT id, role FROM sys_user WHERE phone = '" + phone + "' LIMIT 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(querySql)) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    int role = rs.getInt("role");
                    if (role != 1) {
                        try (Statement updateStmt = conn.createStatement()) {
                            updateStmt.executeUpdate("UPDATE sys_user SET role = 1, admin_type = 0, pond_id = NULL WHERE id = " + id);
                            log.info("商家账号 {} 角色已修正为 merchant", phone);
                        }
                    }
                }
            }
        }

        String querySql = "SELECT id, role FROM sys_user WHERE phone = 'merchant' LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = encoder.encode("merchant");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String insertSql = "INSERT INTO sys_user (phone, nickname, password, role, status, create_time) VALUES ('merchant', '默认商家', '" + password + "', 1, 1, '" + now + "')";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(insertSql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                Long merchantId = keys.getLong(1);
                log.info("创建默认商家账号，ID: {}", merchantId);
                return merchantId;
            }
        }
    }

    private void initDefaultInviteCode(Connection conn, Long merchantId) throws Exception {
        String querySql = "SELECT id FROM invite_code WHERE code = 'FISH2024' LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySql)) {
            if (!rs.next()) {
                String insertSql = "INSERT INTO invite_code (code, merchant_id, used) VALUES ('FISH2024', " + merchantId + ", 0)";
                stmt.executeUpdate(insertSql);
                log.info("创建默认邀请码 FISH2024");
            }
        }
    }

    private void createShopTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            String productsSql = "CREATE TABLE IF NOT EXISTS shop_products (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID（可选）'," +
                    "merchant_id BIGINT NOT NULL COMMENT '所属商家ID'," +
                    "category VARCHAR(50) NOT NULL COMMENT '分类：equipment-钓具/bait-饵料/fish-鱼获/food-菜品'," +
                    "name VARCHAR(100) NOT NULL COMMENT '商品名称'," +
                    "price INT NOT NULL DEFAULT 0 COMMENT '单价（分）'," +
                    "stock INT NOT NULL DEFAULT 0 COMMENT '库存'," +
                    "image_url VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL'," +
                    "description TEXT DEFAULT NULL COMMENT '商品描述'," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'on' COMMENT '状态：on-上架/off-下架'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "INDEX idx_merchant_id (merchant_id)," +
                    "INDEX idx_pond_id (pond_id)," +
                    "INDEX idx_category (category)," +
                    "INDEX idx_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城商品表'";
            stmt.execute(productsSql);

            String ordersSql = "CREATE TABLE IF NOT EXISTS shop_orders (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号'," +
                    "user_id BIGINT NOT NULL COMMENT '下单用户ID'," +
                    "merchant_id BIGINT NOT NULL COMMENT '商家ID'," +
                    "pond_id BIGINT DEFAULT NULL COMMENT '鱼塘ID（可选）'," +
                    "order_type VARCHAR(20) NOT NULL DEFAULT 'shop' COMMENT '订单类型：shop-商城/restaurant-餐饮'," +
                    "total_amount INT NOT NULL DEFAULT 0 COMMENT '订单总金额（分）'," +
                    "status VARCHAR(20) NOT NULL DEFAULT 'pending_pay' COMMENT '状态：pending_pay-待支付/paid-已支付/completed-已完成/cancelled-已取消'," +
                    "spot_id BIGINT DEFAULT NULL COMMENT '配送钓位ID'," +
                    "remark VARCHAR(255) DEFAULT NULL COMMENT '备注'," +
                    "deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "paid_at DATETIME DEFAULT NULL COMMENT '支付时间'," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_merchant_id (merchant_id)," +
                    "INDEX idx_pond_id (pond_id)," +
                    "INDEX idx_status (status)," +
                    "INDEX idx_order_no (order_no)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单表'";
            stmt.execute(ordersSql);

            String itemsSql = "CREATE TABLE IF NOT EXISTS shop_order_items (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "order_id BIGINT NOT NULL COMMENT '订单ID'," +
                    "product_id BIGINT NOT NULL COMMENT '商品ID'," +
                    "product_name VARCHAR(100) DEFAULT NULL COMMENT '商品名称快照'," +
                    "quantity INT NOT NULL DEFAULT 1 COMMENT '数量'," +
                    "unit_price INT NOT NULL DEFAULT 0 COMMENT '下单时单价（分）'," +
                    "subtotal INT NOT NULL DEFAULT 0 COMMENT '小计金额（分）'," +
                    "menu_id BIGINT DEFAULT NULL COMMENT '菜单ID（餐厅兼容）'," +
                    "menu_name VARCHAR(100) DEFAULT NULL COMMENT '商品名称快照（餐厅兼容）'," +
                    "price INT DEFAULT NULL COMMENT '单价（分）（餐厅兼容）'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_order_id (order_id)," +
                    "INDEX idx_product_id (product_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单明细表'";
            stmt.execute(itemsSql);

            addColumnIfNotExists(conn, "shop_orders", "paid_at", "DATETIME");
            addColumnIfNotExists(conn, "shop_order_items", "product_id", "BIGINT");
            addColumnIfNotExists(conn, "shop_order_items", "product_name", "VARCHAR(100)");
            addColumnIfNotExists(conn, "shop_order_items", "unit_price", "INT");
            addColumnIfNotExists(conn, "shop_order_items", "subtotal", "INT");
            addColumnIfNotExists(conn, "shop_order_items", "cooking_method", "VARCHAR(100)");

            log.info("商城表检查/创建完成");
        }
    }

    private void createCatchRecordTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS catch_records (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL COMMENT '用户ID'," +
                "pond_id BIGINT NOT NULL COMMENT '鱼塘ID'," +
                "reservation_id BIGINT DEFAULT NULL COMMENT '预约ID'," +
                "spot_id BIGINT DEFAULT NULL COMMENT '钓位ID'," +
                "fish_type VARCHAR(50) NOT NULL COMMENT '鱼种'," +
                "weight DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '重量（斤）'," +
                "quantity INT NOT NULL DEFAULT 1 COMMENT '数量'," +
                "image_url VARCHAR(500) DEFAULT NULL COMMENT '照片URL'," +
                "status VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理 sold_recycle-已回收 sold_restaurant-已出售 released-放流'," +
                "recycle_price INT DEFAULT NULL COMMENT '回收价格（分）'," +
                "deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_user_id (user_id)," +
                "INDEX idx_pond_id (pond_id)," +
                "INDEX idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渔获记录表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("catch_records 表检查/创建完成");
        }
    }

    private void createRestaurantMenuTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS restaurant_menus (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "pond_id BIGINT NOT NULL COMMENT '鱼塘ID'," +
                "merchant_id BIGINT NOT NULL COMMENT '商家ID'," +
                "name VARCHAR(100) NOT NULL COMMENT '菜品名称'," +
                "category VARCHAR(30) NOT NULL COMMENT '分类：fresh_fish-鲜鱼 cooked-加工菜品 drink-饮品'," +
                "price INT NOT NULL DEFAULT 0 COMMENT '价格（分）'," +
                "stock INT NOT NULL DEFAULT 0 COMMENT '库存：-1表示无限'," +
                "image_url VARCHAR(500) DEFAULT NULL COMMENT '图片URL'," +
                "description TEXT DEFAULT NULL COMMENT '描述'," +
                "cooking_methods TEXT DEFAULT NULL COMMENT '做法选项 JSON，如 [{name:清蒸,price:0},{name:红烧,price:500}]'," +
                "is_special TINYINT NOT NULL DEFAULT 0 COMMENT '是否招牌菜：0-否 1-是'," +
                "status VARCHAR(10) NOT NULL DEFAULT 'on' COMMENT '状态：on-上架 off-下架'," +
                "deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_pond_id (pond_id)," +
                "INDEX idx_merchant_id (merchant_id)," +
                "INDEX idx_category (category)," +
                "INDEX idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐厅菜单表'";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("restaurant_menus 表检查/创建完成");
        }
    }
}
