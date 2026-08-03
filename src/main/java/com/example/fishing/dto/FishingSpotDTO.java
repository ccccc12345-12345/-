package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "钓位请求")
public class FishingSpotDTO {

    @NotBlank(message = "钓位编号不能为空")
    @Schema(description = "钓位编号，如 A01")
    private String spotCode;

    @NotNull(message = "状态不能为空")
    @Schema(description = "0-维修/禁用 1-可用")
    private Integer status;

    @NotNull(message = "鱼塘不能为空")
    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "地图 X 坐标百分比")
    private java.math.BigDecimal coordinateX;

    @Schema(description = "地图 Y 坐标百分比")
    private java.math.BigDecimal coordinateY;
}
