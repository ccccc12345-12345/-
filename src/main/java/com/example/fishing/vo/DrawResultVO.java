package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽号记录视图（含用户与时段信息）
 */
@Data
@Schema(description = "抽号记录视图")
public class DrawResultVO {

    @Schema(description = "抽号ID")
    private Long id;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "时段日期")
    private String slotDate;

    @Schema(description = "场次名称")
    private String slotName;

    @Schema(description = "钓位编号")
    private String spotCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号时间")
    private LocalDateTime drawTime;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;
}
