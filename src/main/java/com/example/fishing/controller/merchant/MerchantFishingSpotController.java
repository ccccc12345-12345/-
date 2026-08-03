package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Result;
import com.example.fishing.dto.BatchSpotCreateDTO;
import com.example.fishing.dto.FishingSpotDTO;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.service.FishingSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 商家钓位管理接口
 */
@Tag(name = "商家钓位管理")
@RestController
@RequestMapping("/api/merchant/fishing-spots")
public class MerchantFishingSpotController extends MerchantBaseController {

    @Autowired
    private FishingSpotService fishingSpotService;

    @GetMapping
    @Operation(summary = "分页查询钓位列表")
    public Result<IPage<FishingSpot>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId) {
        requireMerchantId();
        checkPondOwner(pondId);
        return Result.success(fishingSpotService.pageList(pageNum, pageSize, pondId));
    }

    @PostMapping
    @Operation(summary = "新增钓位")
    public Result<Void> create(@Validated @RequestBody FishingSpotDTO dto) {
        requireMerchantId();
        checkPondOwner(dto.getPondId());
        fishingSpotService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新钓位")
    public Result<Void> update(
            @Parameter(description = "钓位ID") @PathVariable Long id,
            @Validated @RequestBody FishingSpotDTO dto) {
        requireMerchantId();
        FishingSpot spot = fishingSpotService.getById(id);
        if (spot == null) {
            throw new BusinessException("钓位不存在");
        }
        checkPondOwner(spot.getPondId());
        fishingSpotService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除钓位")
    public Result<Void> delete(@Parameter(description = "钓位ID") @PathVariable Long id) {
        requireMerchantId();
        FishingSpot spot = fishingSpotService.getById(id);
        if (spot == null) {
            throw new BusinessException("钓位不存在");
        }
        checkPondOwner(spot.getPondId());
        fishingSpotService.removeById(id);
        return Result.success();
    }

    @PostMapping("/batch")
    @Operation(summary = "批量生成钓位")
    public Result<Void> batchCreate(@Validated @RequestBody BatchSpotCreateDTO dto) {
        requireMerchantId();
        checkPondOwner(dto.getPondId());

        if (dto.getStartNum() == null || dto.getEndNum() == null || dto.getStartNum() > dto.getEndNum()) {
            throw new BusinessException("编号范围不合法");
        }
        if (dto.getEndNum() - dto.getStartNum() > 1000) {
            throw new BusinessException("单次最多生成1000个钓位");
        }

        List<FishingSpot> spots = new ArrayList<>();
        for (int i = dto.getStartNum(); i <= dto.getEndNum(); i++) {
            FishingSpot spot = new FishingSpot();
            spot.setSpotCode(dto.getPrefix() + String.format("%02d", i));
            spot.setStatus(1);
            spot.setPondId(dto.getPondId());
            spots.add(spot);
        }
        fishingSpotService.saveBatch(spots);
        return Result.success();
    }
}
