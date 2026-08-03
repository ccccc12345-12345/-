package com.example.fishing.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.dto.FishingSpotDTO;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.service.FishingSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 钓位管理接口
 */
@Tag(name = "钓位管理")
@RestController
@RequestMapping("/api/admin/fishing-spots")
public class AdminFishingSpotController {

    @Autowired
    private FishingSpotService fishingSpotService;

    @GetMapping
    @Operation(summary = "分页查询钓位列表")
    public Result<IPage<FishingSpot>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId) {
        return Result.success(fishingSpotService.pageList(pageNum, pageSize, pondId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询钓位详情")
    public Result<FishingSpot> get(@Parameter(description = "钓位ID") @PathVariable Long id) {
        return Result.success(fishingSpotService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增钓位")
    public Result<Void> create(@Validated @RequestBody FishingSpotDTO dto) {
        fishingSpotService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新钓位")
    public Result<Void> update(@Parameter(description = "钓位ID") @PathVariable Long id, @Validated @RequestBody FishingSpotDTO dto) {
        fishingSpotService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除钓位")
    public Result<Void> delete(@Parameter(description = "钓位ID") @PathVariable Long id) {
        fishingSpotService.removeById(id);
        return Result.success();
    }
}
