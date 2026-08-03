package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单分页查询参数
 */
@Data
@Schema(description = "订单分页查询参数")
public class ShopOrderQuery {

    @Schema(description = "状态")
    private String status;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
