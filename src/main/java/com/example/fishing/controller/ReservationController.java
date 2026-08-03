package com.example.fishing.controller;

import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Result;
import com.example.fishing.dto.ReservationDTO;
import com.example.fishing.service.ReservationService;
import com.example.fishing.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;

/**
 * 用户预约接口
 */
@Tag(name = "用户预约")
@RestController
@RequestMapping("/api/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "提交预约")
    public Result<Long> book(@Validated @RequestBody ReservationDTO dto) {
        Long reservationId = reservationService.book(CurrentUser.get(), dto);
        return Result.success(reservationId);
    }

    @PostMapping("/direct")
    @Operation(summary = "提交预约并立即分配钓位")
    public Result<ReservationVO> bookDirect(@Validated @RequestBody ReservationDTO dto) {
        return Result.success(reservationService.bookWithSpot(CurrentUser.get(), dto));
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消预约")
    public Result<Void> cancel(@Parameter(description = "预约ID") @PathVariable Long id) {
        reservationService.cancel(CurrentUser.get(), id);
        return Result.success();
    }

    @GetMapping("/my")
    @Operation(summary = "我的预约列表")
    public Result<IPage<ReservationVO>> myReservations() {
        CurrentUser.Context ctx = CurrentUser.getContext();
        Long userId = CurrentUser.get();
        if (ctx == null || userId == null) {
            return Result.success(new Page<>());
        }
        List<ReservationVO> list = reservationService.myReservations(userId);
        IPage<ReservationVO> page = new Page<>();
        page.setRecords(list);
        page.setTotal(list.size());
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的预约详情")
    public Result<ReservationVO> detail(@Parameter(description = "预约ID") @PathVariable Long id) {
        Long userId = CurrentUser.get();
        Optional<ReservationVO> detail = reservationService.myReservations(userId)
                .stream()
                .filter(item -> item.getId() != null && item.getId().equals(id))
                .findFirst();
        if (detail.isPresent()) {
            return Result.success(detail.get());
        }
        throw new BusinessException("预约不存在或无权查看");
    }
}
