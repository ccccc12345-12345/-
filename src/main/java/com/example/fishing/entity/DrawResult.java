package com.example.fishing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽号结果
 */
@Data
@TableName("draw_result")
@Schema(description = "抽号结果")
public class DrawResult {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "预约ID")
    private Long reservationId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "时段ID")
    private Long slotId;

    @Schema(description = "钓位ID")
    private Long spotId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "抽号时间")
    private LocalDateTime drawTime;

    @Schema(description = "鱼塘ID")
    private Long pondId;
}
