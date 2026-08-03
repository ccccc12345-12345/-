package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 批量生成钓位请求
 */
@Data
@Schema(description = "批量生成钓位请求")
public class BatchSpotCreateDTO {

    @NotNull(message = "鱼塘ID不能为空")
    @Schema(description = "鱼塘ID")
    private Long pondId;

    @NotBlank(message = "编号前缀不能为空")
    @Schema(description = "编号前缀，如 A")
    private String prefix;

    @NotNull(message = "起始编号不能为空")
    @Schema(description = "起始编号")
    private Integer startNum;

    @NotNull(message = "结束编号不能为空")
    @Schema(description = "结束编号")
    private Integer endNum;
}
