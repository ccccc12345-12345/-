package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 餐厅菜单
 */
@Data
@TableName("restaurant_menus")
@Schema(description = "餐厅菜单")
public class RestaurantMenu {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "菜品名称")
    private String name;

    @Schema(description = "分类：fresh_fish-鲜鱼 cooked-加工菜品 drink-饮品")
    private String category;

    @Schema(description = "价格（分）")
    private Integer price;

    @Schema(description = "库存：-1 表示无限")
    private Integer stock;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "做法选项（JSON数组字符串），如：[{name:'清蒸',price:0},{name:'红烧',price:500}]")
    private String cookingMethods;

    @Schema(description = "是否招牌：0-否 1-是")
    private Integer isSpecial;

    @Schema(description = "状态：on-上架 off-下架")
    private String status;

    @Schema(description = "逻辑删除：0-未删除 1-已删除")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
