package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 更新员工状态请求
 */
@Data
@Schema(description = "更新员工状态请求")
public class MerchantStaffStatusDTO {

    @NotBlank(message = "状态不能为空")
    @Schema(description = "状态：normal-正常 disabled-禁用")
    private String status;
}
