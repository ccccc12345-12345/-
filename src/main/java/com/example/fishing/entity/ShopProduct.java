package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 商城商品
 */
@Data
@TableName("shop_products")
@Schema(description = "商城商品")
public class ShopProduct {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @NotBlank(message = "商品分类不能为空")
    @Schema(description = "分类：equipment/bait/fish/food")
    private String category;

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称")
    private String name;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格不能为负数")
    @Schema(description = "单价（分）")
    private Integer price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能为负数")
    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "商品图片URL")
    private String imageUrl;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "状态：on-上架/off-下架")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
