package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单项（商城与餐厅复用）
 */
@Data
@TableName("shop_order_items")
@Schema(description = "订单项")
public class ShopOrderItem {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "菜单ID（餐厅订单）")
    private Long menuId;

    @Schema(description = "商品名称快照（餐厅订单）")
    private String menuName;

    @Schema(description = "单价（分）（餐厅订单），含做法加价")
    private Integer price;

    @Schema(description = "所选做法名称（餐厅订单）")
    private String cookingMethod;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "商品ID（商城订单）")
    private Long productId;

    @Schema(description = "商品名称（商城订单）")
    private String productName;

    @Schema(description = "下单时单价（分）（商城订单）")
    private Integer unitPrice;

    @Schema(description = "小计金额（分）（商城订单）")
    private Integer subtotal;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
