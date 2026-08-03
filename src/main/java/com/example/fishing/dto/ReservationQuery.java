package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "预约记录查询")
public class ReservationQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "状态：待抽号/已抽号/预约取消/过期失效")
    private String status;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    @Schema(description = "鱼塘ID")
    private Long pondId;

    @Schema(description = "楸煎ID鍒楄〃")
    private List<Long> pondIds;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
