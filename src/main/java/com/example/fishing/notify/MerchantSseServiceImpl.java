package com.example.fishing.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * 商家 SSE 服务实现
 */
@Service
public class MerchantSseServiceImpl implements MerchantSseService {

    private static final Logger log = LoggerFactory.getLogger(MerchantSseServiceImpl.class);

    @Autowired
    private SseEmitterRepository sseEmitterRepository;

    @Override
    public SseEmitter connect(Long merchantId, String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = UUID.randomUUID().toString().replace("-", "");
        }
        SseEmitter emitter = sseEmitterRepository.register(merchantId, clientId);

        // 发送一次初始连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name(NotificationEventType.DASHBOARD_REFRESH.name())
                    .data(NotificationPayload.of(null, merchantId, null, null, "connected")));
        } catch (Exception e) {
            log.warn("SSE 初始推送失败 merchantId={}", merchantId, e);
        }

        return emitter;
    }

    @Override
    public void sendToMerchant(Long merchantId, NotificationEvent event) {
        if (merchantId == null || event == null) {
            return;
        }
        sseEmitterRepository.broadcast(merchantId, event);
    }

    /**
     * 每 30 秒发送一次心跳，防止 Nginx/代理断开连接
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        for (Long merchantId : sseEmitterRepository.merchantIds()) {
            sseEmitterRepository.sendHeartbeat(merchantId);
        }
    }
}
