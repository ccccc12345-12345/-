package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^(1\\d{10}|merchant)$", message = "手机号格式错误")
    @Schema(description = "手机号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度应为 6-16 位")
    @Schema(description = "密码")
    private String password;

    @Schema(description = "验证码标识")
    private String captchaKey;

    @Size(max = 10, message = "验证码长度不合法")
    @Schema(description = "验证码")
    private String captchaCode;
}
