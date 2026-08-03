package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.service.CatchRecordService;
import com.example.fishing.vo.CatchRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家渔获管理接口（匹配前端 /api/merchant/catches 路径）
 */
@Tag(name = "商家渔获管理")
@RestController
@RequestMapping("/api/merchant/catches")
public class MerchantCatchesController extends MerchantBaseController {

    @Autowired
    private CatchRecordService catchRecordService;

    @GetMapping
    @Operation(summary = "分页查询渔获列表")
    public Result<IPage<CatchRecordVO>> list(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId,
            @Parameter(description = "状态：pending-待处理 recycle_requested-已申请回收 sold_recycle-已回收 sold_restaurant-已入餐厅 released-已放生")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long merchantId = requireMerchantId();
        return Result.success(catchRecordService.queryPage(merchantId, pondId, status, pageNum, pageSize));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新渔获状态（确认回收）")
    public Result<Void> updateStatus(
            @Parameter(description = "渔获记录ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        Long merchantId = requireMerchantId();
        String status = params == null ? null : (String) params.get("status");
        Integer recyclePrice = params == null ? null : (params.get("recyclePrice") instanceof Number
                ? ((Number) params.get("recyclePrice")).intValue()
                : null);
        catchRecordService.confirmRecycle(merchantId, id, recyclePrice);
        return Result.success();
    }
}
