package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 编辑员工请求
 */
@Data
@Schema(description = "编辑员工请求")
public class MerchantStaffUpdateDTO {

    @NotBlank(message = "员工姓名不能为空")
    @Schema(description = "员工姓名")
    private String staffName;

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色：checker-核销员 operator-运营 finance-财务 manager-店长")
    private String role;
}
