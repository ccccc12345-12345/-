package com.example.fishing.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.dto.TimeSlotDTO;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 时段管理接口
 */
@Tag(name = "时段管理")
@RestController
@RequestMapping("/api/admin/time-slots")
public class AdminTimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    @Operation(summary = "分页查询时段列表")
    public Result<IPage<TimeSlot>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId) {
        IPage<TimeSlot> page = timeSlotService.pageList(pageNum, pageSize, pondId);
        timeSlotService.fillRemain(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询时段详情")
    public Result<TimeSlot> get(@Parameter(description = "时段ID") @PathVariable Long id) {
        return Result.success(timeSlotService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增时段")
    public Result<Void> create(@Validated @RequestBody TimeSlotDTO dto) {
        timeSlotService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新时段")
    public Result<Void> update(@Parameter(description = "时段ID") @PathVariable Long id, @Validated @RequestBody TimeSlotDTO dto) {
        timeSlotService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除时段")
    public Result<Void> delete(@Parameter(description = "时段ID") @PathVariable Long id) {
        timeSlotService.deleteSlot(id);
        return Result.success();
    }
}
