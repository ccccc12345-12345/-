package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建商城订单请求
 */
@Data
@Schema(description = "创建商城订单请求")
public class ShopOrderCreateDTO {

    @Schema(description = "订单类型：shop/restaurant")
    private String orderType = "shop";

    @Schema(description = "鱼塘ID（可选）")
    private Long pondId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    @Schema(description = "订单商品明细")
    private List<Item> items;

    @Data
    @Schema(description = "订单商品项")
    public static class Item {

        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为1")
        @Schema(description = "购买数量")
        private Integer quantity;
    }
}
