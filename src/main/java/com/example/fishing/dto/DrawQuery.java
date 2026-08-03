package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "抽号记录查询")
public class DrawQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "钓位ID")
    private Long spotId;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
