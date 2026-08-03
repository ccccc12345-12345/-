package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
@Schema(name = "SysUser", description = "用户实体")
public class SysUser {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "加密密码")
    private String password;

    @Schema(description = "角色：0-普通用户 1-商家 2-平台管理员 3-员工")
    private Integer role;

    @Schema(description = "管理员类型：0-超级管理员 1-普通管理员")
    private Integer adminType;

    @Schema(description = "绑定鱼塘ID（普通管理员生效）")
    private Long pondId;

    @Schema(description = "关联员工表ID")
    private Long staffId;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;
}
