package com.example.fishing.controller;

import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.service.DrawResultService;
import com.example.fishing.vo.DrawResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户抽号接口
 */
@Tag(name = "用户抽号")
@RestController
@RequestMapping("/api/draw")
public class DrawController {

    @Autowired
    private DrawResultService drawResultService;

    @PostMapping("/{reservationId}")
    @Operation(summary = "一键抽号")
    public Result<String> draw(@Parameter(description = "预约ID") @PathVariable Long reservationId) {
        String spotCode = drawResultService.draw(CurrentUser.get(), reservationId);
        return Result.success(spotCode);
    }

    @GetMapping("/my")
    @Operation(summary = "我的抽号记录")
    public Result<List<DrawResultVO>> my() {
        return Result.success(drawResultService.myDraws(CurrentUser.get()));
    }
}
