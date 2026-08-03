package com.example.fishing.notify;

/**
 * 商家端实时通知事件类型
 */
public enum NotificationEventType {

    /**
     * 餐厅订单创建
     */
    RESTAURANT_ORDER_CREATED,

    /**
     * 餐厅订单状态变更
     */
    RESTAURANT_ORDER_STATUS_CHANGED,

    /**
     * 商城订单创建
     */
    SHOP_ORDER_CREATED,

    /**
     * 商城订单状态变更
     */
    SHOP_ORDER_STATUS_CHANGED,

    /**
     * 新预约
     */
    RESERVATION_CREATED,

    /**
     * 预约状态变更（取消、过期、抽号等）
     */
    RESERVATION_STATUS_CHANGED,

    /**
     * 预约核销
     */
    RESERVATION_CHECKED_IN,

    /**
     * 钓位看板变化
     */
    SPOT_BOARD_CHANGED,

    /**
     * 新渔获记录
     */
    CATCH_CREATED,

    /**
     * 商家工作台统计需要刷新
     */
    DASHBOARD_REFRESH
}
