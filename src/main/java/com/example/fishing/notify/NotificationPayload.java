package com.example.fishing.notify;

import java.io.Serializable;

/**
 * 通知事件通用负载，可扩展字段
 */
public class NotificationPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务主键，如订单ID、预约ID
     */
    private Long resourceId;

    /**
     * 关联商家ID
     */
    private Long merchantId;

    /**
     * 关联鱼塘ID
     */
    private Long pondId;

    /**
     * 状态变更后的值
     */
    private String status;

    /**
     * 简要描述
     */
    private String message;

    public NotificationPayload() {
    }

    public static NotificationPayload of(Long resourceId, Long merchantId, Long pondId, String status, String message) {
        NotificationPayload payload = new NotificationPayload();
        payload.setResourceId(resourceId);
        payload.setMerchantId(merchantId);
        payload.setPondId(pondId);
        payload.setStatus(status);
        payload.setMessage(message);
        return payload;
    }

    public static NotificationPayload of(Long resourceId, Long merchantId, Long pondId) {
        return of(resourceId, merchantId, pondId, null, null);
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getPondId() {
        return pondId;
    }

    public void setPondId(Long pondId) {
        this.pondId = pondId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
