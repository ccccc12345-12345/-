package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("shop_orders")
@Schema(description = "订单")
public class ShopOrder {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单类型：shop-商城订单/restaurant-餐厅订单")
    private String orderType;

    @Schema(description = "下单用户ID")
    private Long userId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "配送钓位ID")
    private Long spotId;

    @Schema(description = "订单总金额（分）")
    private Integer totalAmount;

    @Schema(description = "状态：pending_pay-待支付/paid-已支付/completed-已完成/cancelled-已取消")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "逻辑删除：0-未删除 1-已删除")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private LocalDateTime paidAt;
}
