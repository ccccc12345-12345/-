package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 餐厅菜单 DTO
 */
@Data
@Schema(description = "餐厅菜单请求参数")
public class RestaurantMenuDTO {

    @NotNull(message = "鱼塘ID不能为空")
    @Schema(description = "鱼塘ID")
    private Long pondId;

    @NotBlank(message = "菜品名称不能为空")
    @Schema(description = "菜品名称")
    private String name;

    @NotBlank(message = "分类不能为空")
    @Schema(description = "分类：fresh_fish-鲜鱼 cooked-加工菜品 drink-饮品")
    private String category;

    @NotNull(message = "价格不能为空")
    @Schema(description = "基础价格（分），鲜鱼可理解为鱼本身的价格")
    private Integer price;

    @Schema(description = "库存：-1 表示无限")
    private Integer stock;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "描述")
    private String description;

    @Valid
    @Schema(description = "做法选项，鲜鱼/加工菜品可配置多种做法及价格")
    private List<CookingMethodDTO> cookingMethods;

    @Schema(description = "是否招牌：0-否 1-是")
    private Integer isSpecial;

    @Schema(description = "状态：on-上架 off-下架")
    private String status;
}
