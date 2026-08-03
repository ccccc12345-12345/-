package com.example.fishing.controller;

import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.dto.CatchRecordDTO;
import com.example.fishing.service.CatchRecordService;
import com.example.fishing.vo.CatchRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户渔获记录接口
 */
@Tag(name = "用户渔获记录")
@RestController
@RequestMapping("/api/catch")
public class CatchRecordController {

    @Autowired
    private CatchRecordService catchRecordService;

    @PostMapping
    @Operation(summary = "记录渔获")
    public Result<Long> create(@Validated @RequestBody CatchRecordDTO dto) {
        Long recordId = catchRecordService.create(CurrentUser.get(), dto);
        return Result.success(recordId);
    }

    @GetMapping("/my")
    @Operation(summary = "我的渔获列表")
    public Result<List<CatchRecordVO>> myList() {
        return Result.success(catchRecordService.listByUser(CurrentUser.get()));
    }

    @PutMapping("/{id}/request-recycle")
    @Operation(summary = "申请回收")
    public Result<Void> requestRecycle(
            @Parameter(description = "渔获记录ID") @PathVariable Long id) {
        catchRecordService.applyRecycle(CurrentUser.get(), id);
        return Result.success();
    }
}
