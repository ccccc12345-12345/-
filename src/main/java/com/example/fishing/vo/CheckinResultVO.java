package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "核销结果视图")
public class CheckinResultVO {

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "时段日期")
    private String slotDate;

    @Schema(description = "场次名称")
    private String slotName;

    @Schema(description = "钓位编号")
    private String spotCode;

    @Schema(description = "预约状态")
    private String status;

    @Schema(description = "实际收费金额")
    private BigDecimal actualFee;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "核销时间")
    private LocalDateTime checkInTime;
}
