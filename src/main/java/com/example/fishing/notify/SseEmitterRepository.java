package com.example.fishing.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商家 SSE 连接仓库
 *
 * 按 merchantId 分组，每个商家下可存在多个 clientId（多标签页/多员工）。
 */
@Component
public class SseEmitterRepository {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRepository.class);

    /**
     * merchantId -> (clientId -> SseEmitter)
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 注册一个 SSE 连接
     */
    public SseEmitter register(Long merchantId, String clientId) {
        if (merchantId == null || clientId == null) {
            throw new IllegalArgumentException("merchantId 和 clientId 不能为空");
        }

        // 超时设置为 0，表示永不超时，由心跳和前端重连保活
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> remove(merchantId, clientId));
        emitter.onTimeout(() -> remove(merchantId, clientId));
        emitter.onError((e) -> remove(merchantId, clientId));

        emitters.computeIfAbsent(merchantId, k -> new ConcurrentHashMap<>()).put(clientId, emitter);
        log.debug("SSE 注册成功 merchantId={}, clientId={}, 当前连接数={}",
                merchantId, clientId, emitters.get(merchantId).size());
        return emitter;
    }

    /**
     * 移除指定连接
     */
    public void remove(Long merchantId, String clientId) {
        if (merchantId == null || clientId == null) {
            return;
        }
        ConcurrentHashMap<String, SseEmitter> map = emitters.get(merchantId);
        if (map == null) {
            return;
        }
        map.remove(clientId);
        if (map.isEmpty()) {
            emitters.remove(merchantId);
        }
        log.debug("SSE 移除连接 merchantId={}, clientId={}", merchantId, clientId);
    }

    /**
     * 获取所有在线商家ID
     */
    public java.util.Set<Long> merchantIds() {
        return emitters.keySet();
    }

    /**
     * 获取指定商家下所有连接
     */
    public Collection<SseEmitter> findByMerchantId(Long merchantId) {
        if (merchantId == null) {
            return java.util.Collections.emptyList();
        }
        ConcurrentHashMap<String, SseEmitter> map = emitters.get(merchantId);
        if (map == null) {
            return java.util.Collections.emptyList();
        }
        return map.values();
    }

    /**
     * 向指定商家所有连接广播事件
     */
    public void broadcast(Long merchantId, NotificationEvent event) {
        Collection<SseEmitter> list = findByMerchantId(merchantId);
        if (list.isEmpty()) {
            return;
        }

        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .name(event.getType().name())
                .data(event.getPayload());

        for (SseEmitter emitter : list) {
            try {
                emitter.send(builder);
            } catch (IOException e) {
                log.warn("SSE 发送失败，将清理该连接: merchantId={}", merchantId, e);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.warn("SSE 发送异常: merchantId={}", merchantId, e);
            }
        }
    }

    /**
     * 发送心跳保活（SSE comment，不会触发前端 message 事件）
     */
    public void sendHeartbeat(Long merchantId) {
        Collection<SseEmitter> list = findByMerchantId(merchantId);
        if (list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("SSE 心跳发送失败，清理连接: merchantId={}", merchantId);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.warn("SSE 心跳异常: merchantId={}", merchantId, e);
            }
        }
    }
}
