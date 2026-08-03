package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 未参与抽号用户视图
 */
@Data
@Schema(description = "未参与抽号用户视图")
public class MissedDrawVO {

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "时段日期")
    private String slotDate;

    @Schema(description = "场次名称")
    private String slotName;
}
