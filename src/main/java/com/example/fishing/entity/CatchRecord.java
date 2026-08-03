package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渔获记录
 */
@Data
@TableName("catch_records")
@Schema(description = "渔获记录")
public class CatchRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "钓位ID")
    private Long spotId;

    @Schema(description = "鱼种")
    private String fishType;

    @Schema(description = "重量（千克）")
    private BigDecimal weight;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "照片URL")
    private String imageUrl;

    @Schema(description = "状态：pending-待处理/sold_recycle-已回收/sold_restaurant-已售餐厅/released-已放生")
    private String status;

    @Schema(description = "回收价格（分）")
    private Integer recyclePrice;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
