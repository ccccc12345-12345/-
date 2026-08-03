package com.example.fishing.controller;

import com.example.fishing.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局错误页面映射
 * 替代 Spring Boot 默认 Whitelabel Error Page，返回统一 JSON 错误信息。
 */
@Slf4j
@RestController
public class CustomErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    public Result<Void> error(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String uri = (String) request.getAttribute("javax.servlet.error.request_uri");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
        String msg = status == null ? "未知错误" : ("访问失败，状态码：" + status);

        if (throwable != null) {
            log.warn("错误请求 status={}, uri={}, msg={}", status, uri, throwable.getMessage());
        } else {
            log.warn("错误请求 status={}, uri={}", status, uri);
        }

        return Result.error(status == null ? 500 : status, msg);
    }
}
