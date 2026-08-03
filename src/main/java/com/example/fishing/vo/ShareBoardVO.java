package com.example.fishing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 公开分享看板视图
 */
@Data
@Schema(description = "公开分享看板")
public class ShareBoardVO {

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "鱼塘名称")
    private String pondName;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "日期")
    private String slotDate;

    @Schema(description = "场次名称")
    private String slotName;

    @Schema(description = "钓位看板列表")
    private List<SpotBoardVO> spots;
}
