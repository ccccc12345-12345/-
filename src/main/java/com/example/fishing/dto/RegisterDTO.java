package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求 DTO
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长 32 位")
    @Schema(description = "昵称")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{6,16}$", message = "密码需为 6-16 位字母和数字组合")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码")
    private String confirmPassword;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(user|merchant)$", message = "角色类型错误")
    @Schema(description = "角色：user/merchant")
    private String role;

    @Schema(description = "邀请码，商家注册必填")
    private String inviteCode;

    @NotBlank(message = "验证码标识不能为空")
    @Schema(description = "验证码标识")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 10, message = "验证码长度不合法")
    @Schema(description = "验证码")
    private String captchaCode;

    /**
     * 校验两次密码是否一致
     */
    public boolean passwordMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
