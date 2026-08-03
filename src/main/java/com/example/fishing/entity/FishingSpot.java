package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("fishing_spot")
@Schema(description = "钓位")
public class FishingSpot {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "钓位编号，例如 A01")
    private String spotCode;

    @Schema(description = "0禁用，1可用，2维修")
    private Integer status;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "地图 X 坐标百分比")
    private BigDecimal coordinateX;

    @Schema(description = "地图 Y 坐标百分比")
    private BigDecimal coordinateY;
}
