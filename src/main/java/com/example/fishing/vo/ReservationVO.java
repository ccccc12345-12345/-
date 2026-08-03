package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "预约记录视图")
public class ReservationVO {

    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "时段日期")
    private String slotDate;

    @Schema(description = "场次名称")
    private String slotName;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "抽号开始时间")
    private String drawStartTime;

    @Schema(description = "抽号结束时间")
    private String drawEndTime;

    @Schema(description = "钓位编号")
    private String spotCode;

    @Schema(description = "钓位ID")
    private Long spotId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "核销码")
    private String checkinCode;

    @Schema(description = "实际收费金额")
    private BigDecimal actualFee;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "核销时间")
    private LocalDateTime checkInTime;
}
