package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.BusinessException;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 商家时段管理接口
 */
@Tag(name = "商家时段管理")
@RestController
@RequestMapping("/api/merchant/time-slots")
public class MerchantTimeSlotController extends MerchantBaseController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    @Operation(summary = "分页查询时段列表")
    public Result<IPage<TimeSlot>> page(
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "鱼塘ID") @RequestParam(value = "pondId") Long pondId,
            @Parameter(description = "日期，格式 yyyy-MM-dd") @RequestParam(value = "slotDate", required = false) String slotDate) {
        requireMerchantId();
        checkPondOwner(pondId);
        LocalDate parsedSlotDate = parseSlotDate(slotDate);
        IPage<TimeSlot> page = timeSlotService.pageList(pageNum, pageSize, pondId, parsedSlotDate);
        timeSlotService.fillRemain(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询时段详情")
    public Result<TimeSlot> get(@Parameter(description = "时段ID") @PathVariable("id") Long id) {
        requireMerchantId();
        TimeSlot slot = timeSlotService.getById(id);
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        checkPondOwner(slot.getPondId());
        return Result.success(slot);
    }

    @PostMapping
    @Operation(summary = "新增时段")
    public Result<Void> create(@Validated @RequestBody TimeSlotDTO dto) {
        requireMerchantId();
        checkPondOwner(dto.getPondId());
        timeSlotService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新时段")
    public Result<Void> update(
            @Parameter(description = "时段ID") @PathVariable("id") Long id,
            @Validated @RequestBody TimeSlotDTO dto) {
        requireMerchantId();
        TimeSlot slot = timeSlotService.getById(id);
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        checkPondOwner(slot.getPondId());
        checkPondOwner(dto.getPondId());
        timeSlotService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除时段")
    public Result<Void> delete(@Parameter(description = "时段ID") @PathVariable("id") Long id) {
        requireMerchantId();
        TimeSlot slot = timeSlotService.getById(id);
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        checkPondOwner(slot.getPondId());
        // 已有有效订单的场次由服务层拦截，避免直接改动用户预约记录
        timeSlotService.deleteSlot(id);
        return Result.success();
    }

    private LocalDate parseSlotDate(String slotDate) {
        if (slotDate == null || slotDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(slotDate.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd");
        }
    }
}
