package com.example.fishing.common;

public final class Constants {

    private Constants() {
    }

    public static final String RESERVATION_PENDING = "待抽号";
    public static final String RESERVATION_DRAWN = "已抽号";
    public static final String RESERVATION_CANCELLED = "预约取消";
    public static final String RESERVATION_EXPIRED = "过期失效";
    public static final String RESERVATION_CHECKED_IN = "已核销";
    public static final String RESERVATION_VOIDED = "已作废";

    public static final String CATCH_PENDING = "pending";
    public static final String CATCH_RECYCLE_REQUESTED = "recycle_requested";
    public static final String CATCH_SOLD_RECYCLE = "sold_recycle";
    public static final String CATCH_SOLD_RESTAURANT = "sold_restaurant";
    public static final String CATCH_RELEASED = "released";

    public static final Integer SLOT_DISABLED = 0;
    public static final Integer SLOT_ENABLED = 1;

    public static final Integer SPOT_DISABLED = 0;
    public static final Integer SPOT_ENABLED = 1;
    public static final Integer SPOT_MAINTENANCE = 2;

    public static String slotRemainKey(Long slotId) {
        return "slot:remain:" + slotId;
    }

    public static String reservationLockKey(Long slotId, Long userId) {
        return "lock:reservation:" + slotId + ":" + userId;
    }

    public static String drawLockKey(Long userId) {
        return "lock:draw:user:" + userId;
    }

    public static String drawSpotLockKey(Long slotId) {
        return "lock:draw:spot:" + slotId;
    }
}
