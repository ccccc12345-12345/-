package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "收益统计视图")
public class RevenueVO {

    @Schema(description = "日期")
    private String slotDate;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "场次名称")
    private String slotName;

    @Schema(description = "预约人数")
    private Long totalCount;

    @Schema(description = "核销人数")
    private Long checkinCount;

    @Schema(description = "上座率")
    private BigDecimal occupancyRate;

    @Schema(description = "总收入")
    private BigDecimal totalIncome;
}
