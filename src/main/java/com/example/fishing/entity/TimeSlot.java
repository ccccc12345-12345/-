package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("time_slot")
@Schema(description = "时段配置")
public class TimeSlot {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "日期")
    private LocalDate slotDate;

    @Schema(description = "场次名称")
    private String slotName;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "最大预约人数")
    private Integer maxBookings;

    @Schema(description = "提前N天可约")
    private Integer advanceDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号开始时间")
    private LocalDateTime drawStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号结束时间")
    private LocalDateTime drawEndTime;

    @Schema(description = "0-禁用 1-启用")
    private Integer status;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "默认票价")
    private BigDecimal defaultPrice;

    @TableField(exist = false)
    @Schema(description = "剩余名额")
    private Integer remain;
}
