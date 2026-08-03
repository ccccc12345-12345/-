package com.example.fishing.config;

import com.example.fishing.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册登录拦截器
 * 测试阶段临时放行所有接口，仅保留拦截器但不实际拦截，便于联调
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**", "/admin/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/register",
                        "/api/captcha",
                        "/api/captcha/**",
                        "/api/auth/**",
                        "/api/time-slots",
                        "/api/time-slots/**",
                        "/api/ponds",
                        "/api/ponds/**",
                        "/api/share/board",
                        "/test",
                        "/auth/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs/**",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                );
    }
}
