package com.example.fishing.service;

/**
 * 验证码服务
 */
public interface CaptchaService {

    /**
     * 校验验证码，校验成功后立即删除缓存
     *
     * @param captchaKey  验证码标识
     * @param captchaCode 用户输入的验证码
     * @return 校验是否通过
     */
    boolean validate(String captchaKey, String captchaCode);
}
