package com.example.fishing.controller;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.entity.Pond;
import com.example.fishing.service.PondService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 鱼塘管理接口
 */
@Tag(name = "鱼塘管理")
@RestController
@RequestMapping("/api/admin/ponds")
public class PondController {

    @Autowired
    private PondService pondService;

    @GetMapping
    @Operation(summary = "鱼塘列表")
    public Result<List<Pond>> list() {
        return Result.success(pondService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "鱼塘详情")
    public Result<Pond> detail(@Parameter(description = "鱼塘ID") @PathVariable Long id) {
        return Result.success(pondService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增鱼塘")
    public Result<Long> create(@RequestBody Pond pond) {
        checkSuperAdmin();
        pondService.create(pond);
        return Result.success(pond.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改鱼塘")
    public Result<Void> update(
            @Parameter(description = "鱼塘ID") @PathVariable Long id,
            @RequestBody Pond pond) {
        checkSuperAdmin();
        pondService.update(id, pond);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除鱼塘")
    public Result<Void> delete(@Parameter(description = "鱼塘ID") @PathVariable Long id) {
        checkSuperAdmin();
        pondService.delete(id);
        return Result.success();
    }

    private void checkSuperAdmin() {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可操作鱼塘");
        }
    }
}
