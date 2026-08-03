package com.example.fishing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Schema(description = "时段配置请求")
public class TimeSlotDTO {

    @NotNull(message = "日期不能为空")
    @Schema(description = "日期")
    private LocalDate slotDate;

    @NotBlank(message = "场次名称不能为空")
    @Schema(description = "场次名称：早场/午场/晚场/全天场")
    private String slotName;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalTime endTime;

    @NotNull(message = "最大预约数不能为空")
    @Schema(description = "最大预约人数")
    private Integer maxBookings;

    @NotNull(message = "提前天数不能为空")
    @Schema(description = "提前N天可约")
    private Integer advanceDays;

    @NotNull(message = "抽号开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号开始时间")
    private LocalDateTime drawStartTime;

    @NotNull(message = "抽号结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号结束时间")
    private LocalDateTime drawEndTime;

    @NotNull(message = "状态不能为空")
    @Schema(description = "0-禁用 1-启用")
    private Integer status;

    @NotNull(message = "鱼塘不能为空")
    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "默认票价")
    private java.math.BigDecimal defaultPrice;
}
