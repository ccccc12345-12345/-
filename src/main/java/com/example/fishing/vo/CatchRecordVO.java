package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渔获记录视图（含用户、鱼塘、钓位信息）
 */
@Data
@Schema(description = "渔获记录视图")
public class CatchRecordVO {

    @Schema(description = "渔获ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "钓位ID")
    private Long spotId;

    @Schema(description = "钓位编号")
    private String spotCode;

    @Schema(description = "鱼种")
    private String fishType;

    @Schema(description = "重量（千克）")
    private BigDecimal weight;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "照片URL")
    private String imageUrl;

    @Schema(description = "状态：pending/sold_recycle/sold_restaurant/released")
    private String status;

    @Schema(description = "回收价格（分）")
    private Integer recyclePrice;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
