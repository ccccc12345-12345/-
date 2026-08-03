package com.example.fishing.controller.merchant;

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
 * 商家鱼塘管理接口
 */
@Tag(name = "商家鱼塘管理")
@RestController
@RequestMapping("/api/merchant/ponds")
public class MerchantPondController extends MerchantBaseController {

    @Autowired
    private PondService pondService;

    @GetMapping
    @Operation(summary = "查询商家鱼塘列表")
    public Result<List<Pond>> list() {
        Long merchantId = requireMerchantId();
        return Result.success(pondService.listByMerchantId(merchantId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询鱼塘详情")
    public Result<Pond> get(@Parameter(description = "鱼塘ID") @PathVariable Long id) {
        requireMerchantId();
        checkPondOwner(id);
        return Result.success(pondService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增鱼塘")
    public Result<Void> create(@RequestBody Pond pond) {
        requireMerchantOrManager();
        Long merchantId = requireMerchantId();
        pond.setMerchantId(merchantId);
        pondService.create(pond);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改鱼塘")
    public Result<Void> update(
            @Parameter(description = "鱼塘ID") @PathVariable Long id,
            @RequestBody Pond pond) {
        requireMerchantOrManager();
        checkPondOwner(id);
        pondService.update(id, pond);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除鱼塘")
    public Result<Void> delete(@Parameter(description = "鱼塘ID") @PathVariable Long id) {
        requireMerchantOrManager();
        checkPondOwner(id);
        pondService.delete(id);
        return Result.success();
    }
}
