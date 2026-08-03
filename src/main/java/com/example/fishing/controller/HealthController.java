package com.example.fishing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查 / 连通性测试接口
 */
@Tag(name = "健康检查")
@RestController
public class HealthController {

    @GetMapping("/test")
    @Operation(summary = "基础连通性测试")
    public String test() {
        return "服务正常启动";
    }
}
