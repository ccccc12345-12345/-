package com.example.fishing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 餐厅订单项 VO
 */
@Data
@Schema(description = "餐厅订单项")
public class RestaurantOrderItemVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "商品名称快照")
    private String menuName;

    @Schema(description = "单价（分），含做法加价")
    private Integer price;

    @Schema(description = "所选做法名称")
    private String cookingMethod;

    @Schema(description = "数量")
    private Integer quantity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
