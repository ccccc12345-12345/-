package com.example.fishing.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * Redis 通知订阅器
 *
 * 每个后端实例订阅 notify:merchant:* 频道，收到消息后转发给本实例维护的商家 SSE 连接。
 */
@Component
public class RedisNotificationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationListener.class);

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Autowired
    private MerchantSseService merchantSseService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        redisMessageListenerContainer.addMessageListener(this,
                new PatternTopic(RedisNotificationBridge.CHANNEL_PREFIX + "*"));
        log.info("Redis 通知订阅器已启动");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
            if (event.getMerchantId() != null) {
                merchantSseService.sendToMerchant(event.getMerchantId(), event);
                log.debug("Redis 收到通知并转本地 SSE: merchantId={}, type={}",
                        event.getMerchantId(), event.getType());
            }
        } catch (Exception e) {
            log.error("Redis 通知反序列化失败: {}", json, e);
        }
    }
}
