package com.example.fishing.service.impl;

import com.example.fishing.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 验证码服务实现
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";

    @Override
    public boolean validate(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaKey.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
            return false;
        }
        String key = CAPTCHA_PREFIX + captchaKey;
        String cachedCode = redisTemplate.opsForValue().get(key);
        // 无论是否通过，校验后立即删除，防止重放攻击
        redisTemplate.delete(key);
        if (cachedCode == null) {
            log.warn("验证码已过期或不存在: {}", captchaKey);
            return false;
        }
        return cachedCode.equalsIgnoreCase(captchaCode.trim());
    }
}
