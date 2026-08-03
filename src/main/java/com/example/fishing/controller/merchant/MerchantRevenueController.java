package com.example.fishing.controller.merchant;

import com.example.fishing.common.Result;
import com.example.fishing.dto.RevenueQuery;
import com.example.fishing.service.RevenueService;
import com.example.fishing.vo.RevenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商家收益统计接口
 */
@Tag(name = "商家收益统计")
@RestController
@RequestMapping("/api/merchant/revenue")
public class MerchantRevenueController extends MerchantBaseController {

    @Autowired
    private RevenueService revenueService;

    @GetMapping("/summary")
    @Operation(summary = "收入汇总")
    public Result<Map<String, BigDecimal>> summary(
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId) {
        requireMerchantId();
        checkPondOwner(pondId);
        return Result.success(revenueService.summary(pondId));
    }

    @GetMapping("/list")
    @Operation(summary = "收益明细列表")
    public Result<List<RevenueVO>> list(
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        requireMerchantId();
        checkPondOwner(pondId);
        RevenueQuery query = new RevenueQuery();
        query.setPondId(pondId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        return Result.success(revenueService.list(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出收益统计Excel")
    public void export(
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        requireMerchantId();
        checkPondOwner(pondId);
        RevenueQuery query = new RevenueQuery();
        query.setPondId(pondId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        revenueService.exportExcel(query, response);
    }
}
