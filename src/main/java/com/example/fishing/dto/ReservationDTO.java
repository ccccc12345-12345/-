package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "预约请求")
public class ReservationDTO {

    @NotNull(message = "时段ID不能为空")
    @Schema(description = "时段ID")
    private Long slotId;
}
