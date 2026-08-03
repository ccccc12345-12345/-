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
 * 商家渔获回收管理接口
 */
@Tag(name = "商家渔获回收管理")
@RestController
@RequestMapping("/api/merchant/catch")
public class MerchantCatchRecordController extends MerchantBaseController {

    @Autowired
    private CatchRecordService catchRecordService;

    @GetMapping("/pending")
    @Operation(summary = "待处理渔获列表")
    public Result<IPage<CatchRecordVO>> pending(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Long merchantId = requireMerchantId();
        return Result.success(catchRecordService.queryPage(merchantId, null, "pending", pageNum, pageSize));
    }

    @PutMapping("/{id}/recycle")
    @Operation(summary = "确认回收")
    public Result<Void> recycle(
            @Parameter(description = "渔获记录ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> params) {
        Long merchantId = requireMerchantId();
        Integer recyclePrice = params == null ? null : params.get("recyclePrice");
        catchRecordService.confirmRecycle(merchantId, id, recyclePrice);
        return Result.success();
    }
}
