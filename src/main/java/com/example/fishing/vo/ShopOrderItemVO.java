package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单明细 VO
 */
@Data
@Schema(description = "订单明细 VO")
public class ShopOrderItemVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片URL")
    private String productImageUrl;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "下单时单价（分）")
    private Integer unitPrice;

    @Schema(description = "小计金额（分）")
    private Integer subtotal;
}
