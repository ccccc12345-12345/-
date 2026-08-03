package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商家工作台统计视图
 */
@Data
@Schema(description = "商家工作台统计")
public class DashboardStatsVO {

    @Schema(description = "今日收入")
    private BigDecimal todayIncome;

    @Schema(description = "今日预约数")
    private Long todayReservationCount;

    @Schema(description = "今日核销数")
    private Long todayCheckinCount;

    @Schema(description = "今日上座率")
    private BigDecimal occupancyRate;

    @Schema(description = "最近预约记录")
    private List<ReservationVO> recentReservations;
}
