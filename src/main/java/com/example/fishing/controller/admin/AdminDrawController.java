package com.example.fishing.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.dto.DrawQuery;
import com.example.fishing.service.DrawResultService;
import com.example.fishing.vo.DrawResultVO;
import com.example.fishing.vo.MissedDrawVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 抽号记录管理接口
 */
@Tag(name = "抽号记录管理")
@RestController
@RequestMapping("/api/admin/draw-results")
public class AdminDrawController {

    @Autowired
    private DrawResultService drawResultService;

    @GetMapping
    @Operation(summary = "分页查询抽号记录")
    public Result<IPage<DrawResultVO>> page(DrawQuery query) {
        return Result.success(drawResultService.queryPageVo(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询抽号详情")
    public Result<DrawResultVO> get(@Parameter(description = "抽号ID") @PathVariable Long id) {
        DrawQuery query = new DrawQuery();
        query.setPageNum(1);
        query.setPageSize(1);
        IPage<DrawResultVO> page = drawResultService.queryPageVo(query);
        return Result.success(page.getRecords().isEmpty() ? null : page.getRecords().get(0));
    }

    @GetMapping("/missed/{slotId}")
    @Operation(summary = "查询未参与抽号名单")
    public Result<List<MissedDrawVO>> missed(
            @Parameter(description = "时段ID") @PathVariable Long slotId) {
        return Result.success(drawResultService.missedList(slotId));
    }

    @GetMapping("/export")
    @Operation(summary = "导出抽号记录Excel")
    public void export(DrawQuery query, HttpServletResponse response) throws IOException {
        drawResultService.exportExcel(query, response);
    }
}
