package com.example.fishing.notify;

import java.time.LocalDateTime;

/**
 * 商家端实时通知事件
 *
 * 事件对象中显式携带 merchantId/pondId，因为监听线程无法通过 ThreadLocal 获取 CurrentUser。
 *
 * 注意：不继承 ApplicationEvent，避免 Java 17 模块限制导致 Jackson 反序列化 Redis 消息失败。
 */
public class NotificationEvent {

    private final NotificationEventType type;
    private final Long merchantId;
    private final Long pondId;
    private final Long resourceId;
    private final NotificationPayload payload;
    private final LocalDateTime occurAt;

    public NotificationEvent(NotificationEventType type,
                             Long merchantId,
                             Long pondId,
                             Long resourceId,
                             NotificationPayload payload) {
        this.type = type;
        this.merchantId = merchantId;
        this.pondId = pondId;
        this.resourceId = resourceId;
        this.payload = payload;
        this.occurAt = LocalDateTime.now();
    }

    public static NotificationEvent of(NotificationEventType type,
                                       Long merchantId,
                                       Long pondId,
                                       Long resourceId,
                                       NotificationPayload payload) {
        return new NotificationEvent(type, merchantId, pondId, resourceId, payload);
    }

    public static NotificationEvent of(NotificationEventType type,
                                       Long merchantId,
                                       Long pondId,
                                       Long resourceId) {
        return of(type, merchantId, pondId, resourceId,
                NotificationPayload.of(resourceId, merchantId, pondId));
    }

    public static NotificationEvent of(NotificationEventType type,
                                       Long merchantId,
                                       Long pondId,
                                       Long resourceId,
                                       String status,
                                       String message) {
        return of(type, merchantId, pondId, resourceId,
                NotificationPayload.of(resourceId, merchantId, pondId, status, message));
    }

    public NotificationEventType getType() {
        return type;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public Long getPondId() {
        return pondId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public NotificationPayload getPayload() {
        return payload;
    }

    public LocalDateTime getOccurAt() {
        return occurAt;
    }
}
