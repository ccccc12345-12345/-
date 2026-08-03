package com.example.fishing.controller.admin;

import com.example.fishing.common.Result;
import com.example.fishing.dto.RevenueQuery;
import com.example.fishing.service.RevenueService;
import com.example.fishing.vo.RevenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 收益统计管理接口
 */
@Tag(name = "收益统计")
@RestController
@RequestMapping("/api/admin/revenue")
public class AdminRevenueController {

    @Autowired
    private RevenueService revenueService;

    @GetMapping("/summary")
    @Operation(summary = "收入汇总")
    public Result<Map<String, java.math.BigDecimal>> summary(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId) {
        return Result.success(revenueService.summary(pondId));
    }

    @GetMapping("/list")
    @Operation(summary = "收益明细列表")
    public Result<List<RevenueVO>> list(RevenueQuery query) {
        return Result.success(revenueService.list(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出收益统计Excel")
    public void export(RevenueQuery query, HttpServletResponse response) throws IOException {
        revenueService.exportExcel(query, response);
    }
}
