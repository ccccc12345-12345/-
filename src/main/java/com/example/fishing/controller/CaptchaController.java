package com.example.fishing.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.fishing.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码接口
 */
@Slf4j
@Tag(name = "验证码")
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    @GetMapping
    @Operation(summary = "获取图形验证码")
    public Result<Map<String, String>> captcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        // 生成 4 位字符验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode();

        // 存入 Redis，5 分钟过期
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + captchaKey, code.toLowerCase(), CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        Map<String, String> map = new HashMap<>();
        map.put("captchaKey", captchaKey);
        map.put("imageBase64", lineCaptcha.getImageBase64());
        return Result.success(map);
    }
}
