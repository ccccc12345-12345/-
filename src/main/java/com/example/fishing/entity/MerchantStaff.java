package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家员工
 */
@Data
@TableName("merchant_staff")
@Schema(description = "商家员工")
public class MerchantStaff {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属商家ID")
    private Long merchantId;

    @Schema(description = "关联系统用户ID")
    private Long userId;

    @Schema(description = "员工姓名")
    private String staffName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色：checker-核销员 operator-运营 finance-财务 manager-店长")
    private String role;

    @Schema(description = "状态：normal-正常 disabled-禁用")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
