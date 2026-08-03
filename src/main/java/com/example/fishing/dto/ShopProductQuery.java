package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品分页查询参数
 */
@Data
@Schema(description = "商品分页查询参数")
public class ShopProductQuery {

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "状态：on/off")
    private String status;

    @Schema(description = "关键词（名称）")
    private String keyword;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
