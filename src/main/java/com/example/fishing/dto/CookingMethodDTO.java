package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 菜品做法选项
 */
@Data
@Schema(description = "菜品做法选项")
public class CookingMethodDTO {

    @NotBlank(message = "做法名称不能为空")
    @Schema(description = "做法名称，如：清蒸、红烧、酸菜鱼")
    private String name;

    @NotNull(message = "做法价格不能为空")
    @Schema(description = "做法价格（分），0 表示不额外加价")
    private Integer price;
}
