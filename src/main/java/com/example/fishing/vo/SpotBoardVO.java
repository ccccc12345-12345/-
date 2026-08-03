package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "钓位看板视图")
public class SpotBoardVO {

    @Schema(description = "钓位ID")
    private Long spotId;

    @Schema(description = "钓位编号")
    private String spotCode;

    @Schema(description = "钓位原始状态：0禁用，1可用，2维修")
    private Integer spotStatus;

    @Schema(description = "地图 X 坐标百分比")
    private BigDecimal coordinateX;

    @Schema(description = "地图 Y 坐标百分比")
    private BigDecimal coordinateY;

    @Schema(description = "看板状态：free/reserved/using/maintenance/disabled")
    private String status;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户手机号后四位")
    private String userPhone;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "预约状态")
    private String reservationStatus;
}
