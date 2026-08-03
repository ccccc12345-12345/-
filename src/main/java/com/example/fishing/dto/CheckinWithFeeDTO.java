package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 商家核销并设置实际收费请求
 */
@Data
@Schema(description = "商家核销并设置实际收费请求")
public class CheckinWithFeeDTO {

    @NotBlank(message = "核销码不能为空")
    @Schema(description = "核销码")
    private String checkinCode;

    @NotNull(message = "实际收费金额不能为空")
    @Schema(description = "实际收费金额")
    private BigDecimal actualFee;
}
