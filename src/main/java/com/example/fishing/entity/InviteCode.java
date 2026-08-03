package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码
 */
@Data
@TableName("invite_code")
@Schema(description = "邀请码")
public class InviteCode {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "邀请码")
    private String code;

    @Schema(description = "绑定商家ID")
    private Long merchantId;

    @Schema(description = "是否已使用：0-未使用 1-已使用")
    private Integer used;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
