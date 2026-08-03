package com.example.fishing.controller;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.common.Result;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端时段查询接口
 */
@Tag(name = "时段查询")
@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    @Operation(summary = "获取可用时段列表")
    public Result<List<TimeSlot>> list(
            @Parameter(description = "日期，格式 yyyy-MM-dd")
            @RequestParam(value = "slotDate", required = false) String slotDate,
            @Parameter(description = "鱼塘ID")
            @RequestParam(value = "pondId", required = false) Long pondId) {
        LocalDate parsedDate = parseSlotDate(slotDate);
        boolean hasDate = parsedDate != null;
        boolean hasPond = pondId != null && pondId > 0;
        List<TimeSlot> list = timeSlotService.lambdaQuery()
                .eq(hasDate, TimeSlot::getSlotDate, parsedDate)
                .eq(hasPond, TimeSlot::getPondId, pondId)
                .eq(TimeSlot::getStatus, Constants.SLOT_ENABLED)
                .ge(!hasDate, TimeSlot::getSlotDate, LocalDate.now())
                .orderByAsc(TimeSlot::getSlotDate)
                .orderByAsc(TimeSlot::getStartTime)
                .list();
        list = list.stream()
                .filter(this::isVisibleToUser)
                .collect(Collectors.toList());
        // 填充 Redis 实时剩余名额
        timeSlotService.fillRemain(list);
        return Result.success(list);
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

    private boolean isVisibleToUser(TimeSlot slot) {
        if (slot == null || !Constants.SLOT_ENABLED.equals(slot.getStatus())) {
            return false;
        }
        if (slot.getSlotDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (slot.getSlotDate().isBefore(today)) {
            return false;
        }
        LocalTime nowTime = LocalTime.now();
        if (slot.getSlotDate().isEqual(today) && !slot.getStartTime().isAfter(nowTime)) {
            return false;
        }
        int advanceDays = slot.getAdvanceDays() == null ? 0 : slot.getAdvanceDays();
        if (slot.getSlotDate().isAfter(today.plusDays(advanceDays))) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return slot.getDrawEndTime() == null || !slot.getDrawEndTime().isBefore(now);
    }
}
