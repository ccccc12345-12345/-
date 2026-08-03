package com.example.fishing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "核销请求")
public class CheckinDTO {

    @NotBlank(message = "核销码不能为空")
    @Schema(description = "核销码")
    private String checkinCode;
}
