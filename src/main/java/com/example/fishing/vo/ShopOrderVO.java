package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商城订单 VO
 */
@Data
@Schema(description = "商城订单 VO")
public class ShopOrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "订单类型：shop/restaurant")
    private String orderType;

    @Schema(description = "订单总金额（分）")
    private Integer totalAmount;

    @Schema(description = "状态：pending_pay/paid/completed/cancelled")
    private String status;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "订单明细")
    private List<ShopOrderItemVO> items;
}
