package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 渔获记录创建请求
 */
@Data
@Schema(description = "渔获记录创建请求")
public class CatchRecordDTO {

    @NotNull(message = "鱼塘ID不能为空")
    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "钓位ID")
    private Long spotId;

    @NotBlank(message = "鱼种不能为空")
    @Schema(description = "鱼种")
    private String fishType;

    @NotNull(message = "重量不能为空")
    @Schema(description = "重量（千克）")
    private BigDecimal weight;

    @NotNull(message = "数量不能为空")
    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "照片URL")
    private String imageUrl;
}
