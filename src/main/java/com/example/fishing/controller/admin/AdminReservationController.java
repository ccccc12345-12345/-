package com.example.fishing.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.dto.ReservationQuery;
import com.example.fishing.service.ReservationService;
import com.example.fishing.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 预约记录管理接口
 */
@Tag(name = "预约记录管理")
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    @Operation(summary = "分页查询预约记录")
    public Result<IPage<ReservationVO>> page(ReservationQuery query) {
        return Result.success(reservationService.queryPageVo(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预约详情")
    public Result<ReservationVO> get(@Parameter(description = "预约ID") @PathVariable Long id) {
        ReservationQuery query = new ReservationQuery();
        query.setUserId(id);
        query.setPageNum(1);
        query.setPageSize(1);
        IPage<ReservationVO> page = reservationService.queryPageVo(query);
        return Result.success(page.getRecords().isEmpty() ? null : page.getRecords().get(0));
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "手动取消预约")
    public Result<Void> adminCancel(
            @Parameter(description = "预约ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> params) {
        String reason = params == null ? null : params.get("reason");
        reservationService.adminCancel(id, reason);
        return Result.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出预约记录Excel")
    public void export(ReservationQuery query, HttpServletResponse response) throws IOException {
        reservationService.exportExcel(query, response);
    }

    @PutMapping("/actual-fee/{id}")
    @Operation(summary = "修改实际收费金额")
    public Result<Void> updateActualFee(
            @Parameter(description = "预约ID") @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> params) {
        BigDecimal actualFee = params == null ? null : params.get("actualFee");
        reservationService.updateActualFee(id, actualFee);
        return Result.success();
    }
}
