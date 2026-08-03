package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建餐厅订单 DTO
 */
@Data
@Schema(description = "创建餐厅订单请求参数")
public class CreateRestaurantOrderDTO {

    @Schema(description = "配送钓位ID")
    private Long spotId;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "订单项不能为空")
    @Valid
    @Schema(description = "订单项列表")
    private List<RestaurantOrderItemDTO> items;
}
