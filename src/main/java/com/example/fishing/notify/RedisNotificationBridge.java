package com.example.fishing.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 通知桥接器
 *
 * 将本地通知事件序列化后发布到 Redis 频道，频道按商家隔离。
 */
@Component
public class RedisNotificationBridge {

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationBridge.class);

    public static final String CHANNEL_PREFIX = "notify:merchant:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publish(NotificationEvent event) {
        if (event == null || event.getMerchantId() == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            String channel = CHANNEL_PREFIX + event.getMerchantId();
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug("Redis 广播通知: channel={}, type={}", channel, event.getType());
        } catch (JsonProcessingException e) {
            log.error("通知事件序列化失败: type={}", event.getType(), e);
        }
    }
}
