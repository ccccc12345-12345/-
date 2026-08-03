package com.example.fishing.controller;

import com.example.fishing.common.Result;
import com.example.fishing.dto.CheckinDTO;
import com.example.fishing.service.CheckinService;
import com.example.fishing.vo.CheckinResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 核销接口
 */
@Tag(name = "核销管理")
@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    @PostMapping
    @Operation(summary = "核销预约")
    public Result<CheckinResultVO> checkin(@RequestBody @Validated CheckinDTO dto) {
        CheckinResultVO vo = checkinService.checkin(dto.getCheckinCode());
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "根据核销码查询预约")
    public Result<CheckinResultVO> query(@RequestParam String checkinCode) {
        CheckinResultVO vo = checkinService.queryByCode(checkinCode);
        return Result.success(vo);
    }
}
