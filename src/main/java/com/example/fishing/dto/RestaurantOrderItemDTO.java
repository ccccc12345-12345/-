package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 餐厅订单项 DTO
 */
@Data
@Schema(description = "餐厅订单项")
public class RestaurantOrderItemDTO {

    @NotNull(message = "菜单ID不能为空")
    @Schema(description = "菜单ID")
    private Long menuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "所选做法名称，不传则使用菜品基础价格")
    private String cookingMethod;
}
