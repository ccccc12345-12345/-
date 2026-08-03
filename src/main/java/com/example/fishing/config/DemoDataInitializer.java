package com.example.fishing.config;

import com.example.fishing.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            relaxRestaurantItemSchema();
            Long merchantId = ensureUser("18800000001", "老板账号", "123456", 1);
            Long userId = ensureUser("18800000002", "体验用户", "123456", 0);
            List<Long> pondIds = ensurePonds(merchantId);
            ensureSpots(pondIds);
            List<Long> slotIds = ensureFutureSlots(pondIds);
            ensureRestaurantMenus(merchantId, pondIds);
            ensureShopProducts(merchantId, pondIds);
            ensureSampleReservations(userId, slotIds);
            ensureSampleCatches(userId, pondIds);
            ensureSampleRestaurantOrders(userId, merchantId, pondIds);
            log.info("Demo data ready. boss=18800000001/123456, user=18800000002/123456");
        } catch (Exception e) {
            log.warn("Demo data initializer skipped: {}", e.getMessage(), e);
        }
    }

    private void relaxRestaurantItemSchema() {
        executeIgnore("ALTER TABLE shop_order_items MODIFY product_id BIGINT DEFAULT NULL");
        executeIgnore("ALTER TABLE shop_order_items MODIFY unit_price INT DEFAULT NULL");
        executeIgnore("ALTER TABLE shop_order_items MODIFY subtotal INT DEFAULT NULL");
    }

    private Long ensureUser(String phone, String nickname, String password, int role) {
        Long existing = queryLong("SELECT id FROM sys_user WHERE phone = ? LIMIT 1", phone);
        if (existing != null) {
            jdbcTemplate.update("UPDATE sys_user SET nickname = ?, role = ?, status = 1 WHERE id = ?", nickname, role, existing);
            return existing;
        }
        return insertAndReturnId(
                "INSERT INTO sys_user (phone, nickname, password, role, status, create_time) VALUES (?, ?, ?, ?, 1, ?)",
                phone, nickname, passwordEncoder.encode(password), role, LocalDateTime.now());
    }

    private List<Long> ensurePonds(Long merchantId) {
        Object[][] data = {
                {"青岩湖精品钓场", "lake", "杭州市余杭区青岩湖路 88 号", "0571-88000001", "/demo-assets/ponds/pond-1.svg", "/demo-assets/ponds/map-1.svg"},
                {"澄湾生态鱼塘", "pond", "苏州市吴中区澄湾大道 26 号", "0512-66000002", "/demo-assets/ponds/pond-2.svg", "/demo-assets/ponds/map-2.svg"},
                {"海风礁岸路亚营地", "sea", "宁波市象山县礁岸路 9 号", "0574-77000003", "/demo-assets/ponds/pond-3.svg", "/demo-assets/ponds/map-3.svg"}
        };
        List<Long> ids = new ArrayList<>();
        for (Object[] item : data) {
            String name = (String) item[0];
            Long id = queryLong("SELECT id FROM pond WHERE name = ? LIMIT 1", name);
            if (id == null) {
                id = insertAndReturnId(
                        "INSERT INTO pond (name, category, address, phone, merchant_id, status, cover_image, floor_plan_url, booking_notice, cancel_rule, refund_rule, create_time, update_time) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?)",
                        name, item[1], item[2], item[3], merchantId, item[4], item[5],
                        "请按预约时间到场，预约成功后系统会直接分配钓位，到场出示核销码签到。",
                        "开场前可取消；已核销或已开场订单请联系商家处理。",
                        "符合取消规则的订单按原路退回，活动或套餐以页面说明为准。",
                        LocalDateTime.now(), LocalDateTime.now());
            } else {
                jdbcTemplate.update("UPDATE pond SET merchant_id = ?, status = 1, category = ?, address = ?, phone = ?, cover_image = ?, floor_plan_url = ?, update_time = ? WHERE id = ?",
                        merchantId, item[1], item[2], item[3], item[4], item[5], LocalDateTime.now(), id);
            }
            ids.add(id);
        }
        return ids;
    }

    private void ensureSpots(List<Long> pondIds) {
        for (int p = 0; p < pondIds.size(); p++) {
            Long pondId = pondIds.get(p);
            for (int i = 1; i <= 10; i++) {
                String code = "P" + (p + 1) + "-" + ((char) ('A' + p)) + String.format("%02d", i);
                Long existing = queryLong("SELECT id FROM fishing_spot WHERE pond_id = ? AND spot_code = ? LIMIT 1", pondId, code);
                BigDecimal x = BigDecimal.valueOf(12 + (long) ((i - 1) % 5) * 18);
                BigDecimal y = BigDecimal.valueOf(18 + (long) ((i - 1) / 5) * 48 + (i % 2) * 5L);
                int status = i == 10 ? Constants.SPOT_MAINTENANCE : Constants.SPOT_ENABLED;
                if (existing == null) {
                    jdbcTemplate.update("INSERT INTO fishing_spot (spot_code, status, pond_id, coordinate_x, coordinate_y) VALUES (?, ?, ?, ?, ?)",
                            code, status, pondId, x, y);
                } else {
                    jdbcTemplate.update("UPDATE fishing_spot SET status = ?, coordinate_x = ?, coordinate_y = ? WHERE id = ?",
                            status, x, y, existing);
                }
            }
        }
    }

    private List<Long> ensureFutureSlots(List<Long> pondIds) {
        List<Long> ids = new ArrayList<>();
        String[] names = {"早场", "午场", "晚场", "全天场"};
        LocalTime[][] times = {
                {LocalTime.of(7, 0), LocalTime.of(11, 30)},
                {LocalTime.of(12, 30), LocalTime.of(17, 0)},
                {LocalTime.of(18, 0), LocalTime.of(22, 0)},
                {LocalTime.of(8, 0), LocalTime.of(18, 0)}
        };
        for (int i = 0; i < 12; i++) {
            Long pondId = pondIds.get(i % pondIds.size());
            int type = i % names.length;
            LocalDate date = LocalDate.now().plusDays(1 + (i / 3));
            LocalDateTime drawStart = LocalDateTime.now().minusHours(1);
            LocalDateTime drawEnd = LocalDateTime.of(date, times[type][0]).minusHours(1);
            if (!drawEnd.isAfter(LocalDateTime.now())) {
                drawEnd = LocalDateTime.now().plusHours(4);
            }

            Long existing = queryLong("SELECT id FROM time_slot WHERE pond_id = ? AND slot_date = ? AND slot_name = ? LIMIT 1",
                    pondId, date, names[type]);
            if (existing == null) {
                existing = insertAndReturnId(
                        "INSERT INTO time_slot (pond_id, slot_date, slot_name, start_time, end_time, max_bookings, advance_days, draw_start_time, draw_end_time, status, default_price) VALUES (?, ?, ?, ?, ?, 18, 30, ?, ?, 1, ?)",
                        pondId, date, names[type], times[type][0], times[type][1], drawStart, drawEnd,
                        BigDecimal.valueOf(9800 + (long) (i % 4) * 2000));
            } else {
                jdbcTemplate.update("UPDATE time_slot SET start_time = ?, end_time = ?, max_bookings = 18, advance_days = 30, draw_start_time = ?, draw_end_time = ?, status = 1, default_price = ? WHERE id = ?",
                        times[type][0], times[type][1], drawStart, drawEnd,
                        BigDecimal.valueOf(9800 + (long) (i % 4) * 2000), existing);
            }
            ids.add(existing);
        }
        return ids;
    }

    private void ensureRestaurantMenus(Long merchantId, List<Long> pondIds) {
        Object[][] data = {
                {"清蒸鲜鲈鱼", "fresh_fish", 6800, 20, "/demo-assets/restaurant/dish-1.svg", 1, "[{\"name\":\"清蒸\",\"price\":0},{\"name\":\"红烧\",\"price\":500},{\"name\":\"酸菜鱼\",\"price\":1500}]"},
                {"砂锅鱼头煲", "cooked", 9800, 18, "/demo-assets/restaurant/dish-2.svg", 1, "[{\"name\":\"砂锅煲\",\"price\":0},{\"name\":\"加辣煲\",\"price\":300}]"},
                {"香煎小黄鱼", "fresh_fish", 6800, 30, "/demo-assets/restaurant/dish-3.svg", 0, "[{\"name\":\"香煎\",\"price\":0},{\"name\":\"椒盐\",\"price\":0}]"},
                {"藤椒鱼片", "cooked", 8800, 25, "/demo-assets/restaurant/dish-4.svg", 1, "[{\"name\":\"藤椒味\",\"price\":0},{\"name\":\"麻辣味\",\"price\":200}]"},
                {"酱烤鲫鱼", "cooked", 5200, 30, "/demo-assets/restaurant/dish-5.svg", 0, "[{\"name\":\"酱烤\",\"price\":0},{\"name\":\"炭烤\",\"price\":400}]"},
                {"现磨冷萃茶", "drink", 1800, 60, "/demo-assets/restaurant/drink-1.svg", 0, null},
                {"青柠苏打", "drink", 1600, 60, "/demo-assets/restaurant/drink-2.svg", 0, null},
                {"鱼汤面", "cooked", 4800, 35, "/demo-assets/restaurant/dish-6.svg", 0, "[{\"name\":\"标准\",\"price\":0},{\"name\":\"加蛋\",\"price\":200}]"},
                {"椒盐鱼块", "cooked", 5800, 30, "/demo-assets/restaurant/dish-7.svg", 0, "[{\"name\":\"椒盐\",\"price\":0},{\"name\":\"孜然\",\"price\":100}]"},
                {"鲜鱼刺身拼盘", "fresh_fish", 16800, 12, "/demo-assets/restaurant/dish-8.svg", 1, "[{\"name\":\"刺身\",\"price\":0},{\"name\":\"薄切\",\"price\":0},{\"name\":\"厚切\",\"price\":500}]"}
        };
        for (int i = 0; i < data.length; i++) {
            Long pondId = pondIds.get(i % pondIds.size());
            String name = (String) data[i][0];
            Long existing = queryLong("SELECT id FROM restaurant_menus WHERE pond_id = ? AND name = ? LIMIT 1", pondId, name);
            String cookingMethods = (String) data[i][6];
            if (existing == null) {
                jdbcTemplate.update("INSERT INTO restaurant_menus (pond_id, merchant_id, name, category, price, stock, image_url, description, cooking_methods, is_special, status, deleted, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'on', 0, ?, ?)",
                        pondId, merchantId, name, data[i][1], data[i][2], data[i][3], data[i][4],
                        "鱼塘餐厅现点现做，可绑定钓位配送。", cookingMethods, data[i][5], LocalDateTime.now(), LocalDateTime.now());
            } else {
                jdbcTemplate.update("UPDATE restaurant_menus SET price = ?, stock = ?, image_url = ?, cooking_methods = ?, status = 'on', deleted = 0, update_time = ? WHERE id = ?",
                        data[i][2], data[i][3], data[i][4], cookingMethods, LocalDateTime.now(), existing);
            }
        }
    }

    private void ensureShopProducts(Long merchantId, List<Long> pondIds) {
        Object[][] data = {
                {"碳素手竿 5.4m", "equipment", 36900, 18, "/demo-assets/shop/product-1.svg"},
                {"路亚亮片套装", "equipment", 8900, 40, "/demo-assets/shop/product-2.svg"},
                {"湖库综合饵", "bait", 2600, 100, "/demo-assets/shop/product-3.svg"},
                {"红虫颗粒", "bait", 3200, 80, "/demo-assets/shop/product-4.svg"},
                {"活水鲈鱼", "fish", 6800, 24, "/demo-assets/shop/product-5.svg"},
                {"生态草鱼", "fish", 4200, 30, "/demo-assets/shop/product-6.svg"},
                {"鱼汤套餐券", "food", 5800, 60, "/demo-assets/shop/product-7.svg"},
                {"钓箱坐垫", "equipment", 12900, 25, "/demo-assets/shop/product-8.svg"},
                {"防晒钓鱼帽", "equipment", 5900, 50, "/demo-assets/shop/product-9.svg"},
                {"夜钓灯", "equipment", 19900, 16, "/demo-assets/shop/product-10.svg"}
        };
        for (int i = 0; i < data.length; i++) {
            String name = (String) data[i][0];
            Long existing = queryLong("SELECT id FROM shop_products WHERE name = ? LIMIT 1", name);
            if (existing == null) {
                jdbcTemplate.update("INSERT INTO shop_products (pond_id, merchant_id, category, name, price, stock, image_url, description, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'on', ?, ?)",
                        pondIds.get(i % pondIds.size()), merchantId, data[i][1], name, data[i][2], data[i][3], data[i][4],
                        "精选营地商品，库存和订单均由后端同步。", LocalDateTime.now(), LocalDateTime.now());
            } else {
                jdbcTemplate.update("UPDATE shop_products SET price = ?, stock = ?, image_url = ?, status = 'on', update_time = ? WHERE id = ?",
                        data[i][2], data[i][3], data[i][4], LocalDateTime.now(), existing);
            }
        }
    }

    private void ensureSampleReservations(Long userId, List<Long> slotIds) {
        if (queryInt("SELECT COUNT(*) FROM reservation WHERE user_id = ? AND status = ?", userId, Constants.RESERVATION_DRAWN) >= 6) {
            return;
        }
        for (int i = 0; i < Math.min(6, slotIds.size()); i++) {
            Long slotId = slotIds.get(i);
            Long pondId = queryLong("SELECT pond_id FROM time_slot WHERE id = ?", slotId);
            Long spotId = queryLong("SELECT id FROM fishing_spot WHERE pond_id = ? AND status = 1 ORDER BY id LIMIT 1 OFFSET " + (i % 6), pondId);
            Long reservationId = insertAndReturnId(
                    "INSERT INTO reservation (user_id, slot_id, pond_id, status, create_time, checkin_code) VALUES (?, ?, ?, ?, ?, ?)",
                    userId, slotId, pondId, Constants.RESERVATION_DRAWN, LocalDateTime.now().minusHours(i), String.valueOf(600000 + i));
            if (spotId != null) {
                jdbcTemplate.update("INSERT INTO draw_result (reservation_id, user_id, slot_id, spot_id, pond_id, draw_time) VALUES (?, ?, ?, ?, ?, ?)",
                        reservationId, userId, slotId, spotId, pondId, LocalDateTime.now().minusHours(i));
            }
        }
    }

    private void ensureSampleCatches(Long userId, List<Long> pondIds) {
        if (queryInt("SELECT COUNT(*) FROM catch_records WHERE user_id = ?", userId) >= 6) {
            return;
        }
        String[] fish = {"鲈鱼", "草鱼", "鲫鱼", "鳜鱼", "青鱼", "黄颡鱼"};
        for (int i = 0; i < fish.length; i++) {
            Long pondId = pondIds.get(i % pondIds.size());
            Long spotId = queryLong("SELECT id FROM fishing_spot WHERE pond_id = ? ORDER BY id LIMIT 1 OFFSET " + (i % 5), pondId);
            jdbcTemplate.update("INSERT INTO catch_records (user_id, pond_id, reservation_id, spot_id, fish_type, weight, quantity, image_url, status, recycle_price, create_time, update_time) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    userId, pondId, spotId, fish[i], BigDecimal.valueOf(1.2 + i * 0.35), 1 + i % 3,
                    "/demo-assets/catches/catch-" + ((i % 3) + 1) + ".svg",
                    i % 3 == 0 ? Constants.CATCH_RECYCLE_REQUESTED : Constants.CATCH_PENDING,
                    i % 3 == 0 ? 3600 + i * 500 : null,
                    LocalDateTime.now().minusDays(i), LocalDateTime.now().minusDays(i));
        }
    }

    private void ensureSampleRestaurantOrders(Long userId, Long merchantId, List<Long> pondIds) {
        if (queryInt("SELECT COUNT(*) FROM shop_orders WHERE user_id = ? AND order_type = 'restaurant'", userId) >= 4) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Long pondId = pondIds.get(i % pondIds.size());
            Long menuId = queryLong("SELECT id FROM restaurant_menus WHERE pond_id = ? ORDER BY id LIMIT 1", pondId);
            Integer price = menuId == null ? 0 : queryInteger("SELECT price FROM restaurant_menus WHERE id = ?", menuId);
            Long spotId = queryLong("SELECT id FROM fishing_spot WHERE pond_id = ? ORDER BY id LIMIT 1", pondId);
            Long orderId = insertAndReturnId(
                    "INSERT INTO shop_orders (order_no, user_id, merchant_id, pond_id, order_type, total_amount, status, spot_id, remark, deleted, create_time, update_time, paid_at) VALUES (?, ?, ?, ?, 'restaurant', ?, ?, ?, ?, 0, ?, ?, ?)",
                    "RDEMO" + System.currentTimeMillis() + i, userId, merchantId, pondId, price == null ? 0 : price,
                    i % 2 == 0 ? "cooking" : "completed", spotId, "演示订单", LocalDateTime.now().minusDays(i), LocalDateTime.now().minusDays(i), LocalDateTime.now().minusDays(i));
            if (menuId != null) {
                jdbcTemplate.update("INSERT INTO shop_order_items (order_id, menu_id, menu_name, price, quantity, create_time) SELECT ?, id, name, price, 1, ? FROM restaurant_menus WHERE id = ?",
                        orderId, LocalDateTime.now().minusDays(i), menuId);
            }
        }
    }

    private Long queryLong(String sql, Object... args) {
        List<Long> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return values.isEmpty() ? null : values.get(0);
    }

    private Integer queryInteger(String sql, Object... args) {
        List<Integer> values = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt(1), args);
        return values.isEmpty() ? null : values.get(0);
    }

    private int queryInt(String sql, Object... args) {
        Integer value = queryInteger(sql, args);
        return value == null ? 0 : value;
    }

    private Long insertAndReturnId(String sql, Object... args) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private void executeIgnore(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            log.debug("Ignored SQL: {}", sql);
        }
    }
}
