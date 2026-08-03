package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Result;
import com.example.fishing.dto.ReservationQuery;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.Reservation;
import com.example.fishing.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家预约记录管理接口
 */
@Tag(name = "商家预约记录管理")
@RestController
@RequestMapping("/api/merchant/reservations")
public class MerchantReservationController extends MerchantBaseController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    @Operation(summary = "分页查询预约记录")
    public Result<IPage<com.example.fishing.vo.ReservationVO>> page(
            @Parameter(description = "鱼塘ID") @RequestParam(value = "pondId", required = false) Long pondId,
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false) String status) {
        ReservationQuery query = new ReservationQuery();
        applyPondScope(pondId, query);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setStatus(status);
        return Result.success(reservationService.queryPageVo(query));
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消预约")
    public Result<Void> cancel(
            @Parameter(description = "预约ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> params) {
        requireMerchantId();
        Reservation reservation = reservationService.getById(id);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        checkPondOwner(reservation.getPondId());
        String reason = params == null ? null : params.get("reason");
        reservationService.adminCancel(id, reason);
        return Result.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出预约记录Excel")
    public void export(
            @Parameter(description = "鱼塘ID") @RequestParam(value = "pondId", required = false) Long pondId,
            @Parameter(description = "开始日期") @RequestParam(value = "startDate", required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(value = "endDate", required = false) String endDate,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false) String status,
            HttpServletResponse response) throws IOException {
        ReservationQuery query = new ReservationQuery();
        applyPondScope(pondId, query);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setStatus(status);
        reservationService.exportExcel(query, response);
    }

    private void applyPondScope(Long pondId, ReservationQuery query) {
        Long merchantId = requireMerchantId();
        if (pondId != null) {
            pondService.checkMerchantOwner(pondId, merchantId);
            query.setPondId(pondId);
            return;
        }
        List<Long> pondIds = pondService.listByMerchantId(merchantId)
                .stream()
                .map(Pond::getId)
                .collect(Collectors.toList());
        query.setPondIds(pondIds);
    }
}
