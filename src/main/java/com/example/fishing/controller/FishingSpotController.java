package com.example.fishing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.service.FishingSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端钓位查询接口
 */
@Tag(name = "钓位查询")
@RestController
@RequestMapping("/api/fishing-spots")
public class FishingSpotController {

    @Autowired
    private FishingSpotService fishingSpotService;

    @GetMapping
    @Operation(summary = "获取可用钓位列表")
    public Result<IPage<FishingSpot>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "100") Integer pageSize,
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId) {
        return Result.success(fishingSpotService.pageList(pageNum, pageSize, pondId));
    }
}
