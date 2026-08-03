package com.example.fishing.notify;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 商家 SSE 服务
 */
public interface MerchantSseService {

    /**
     * 建立 SSE 连接
     *
     * @param merchantId 商家ID
     * @param clientId   客户端唯一标识，为空则自动生成
     * @return SseEmitter
     */
    SseEmitter connect(Long merchantId, String clientId);

    /**
     * 向指定商家所有在线连接推送事件
     */
    void sendToMerchant(Long merchantId, NotificationEvent event);
}
