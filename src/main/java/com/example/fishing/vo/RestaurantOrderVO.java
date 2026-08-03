package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 餐厅订单 VO
 */
@Data
@Schema(description = "餐厅订单")
public class RestaurantOrderVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单类型：restaurant-餐厅订单")
    private String orderType;

    @Schema(description = "下单用户ID")
    private Long userId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "配送钓位ID")
    private Long spotId;

    @Schema(description = "配送钓位编号")
    private String spotCode;

    @Schema(description = "订单总金额（分）")
    private Integer totalAmount;

    @Schema(description = "状态：pending-待处理 accepted-已接单 cooking-制作中 delivered-已配送 completed-已完成 cancelled-已取消")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "下单用户昵称")
    private String userNickname;

    @Schema(description = "下单用户手机号")
    private String userPhone;

    @Schema(description = "订单项列表")
    private List<RestaurantOrderItemVO> items;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
