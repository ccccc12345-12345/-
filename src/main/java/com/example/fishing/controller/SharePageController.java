package com.example.fishing.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

/**
 * 分享页面入口
 * 由于前端为独立 SPA，/share 路由由前端渲染；
 * 此处对直接访问后端 /share 的请求做 302 重定向到前端地址，避免 404。
 */
@Slf4j
@Controller
public class SharePageController {

    @Value("${app.frontend-url:http://localhost:3001}")
    private String frontendUrl;

    @GetMapping("/share")
    public RedirectView share(
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long slotId,
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        String query = request.getQueryString();
        String target = frontendUrl + "/share" + (query != null && !query.isEmpty() ? "?" + query : "");
        log.info("分享页面入口重定向: client={}, target={}", request.getRemoteAddr(), target);
        return new RedirectView(target);
    }
}
