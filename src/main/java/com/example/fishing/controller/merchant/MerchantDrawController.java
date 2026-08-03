package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Result;
import com.example.fishing.dto.DrawQuery;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.service.DrawResultService;
import com.example.fishing.service.TimeSlotService;
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
 * 商家抽号记录管理接口
 */
@Tag(name = "商家抽号记录管理")
@RestController
@RequestMapping("/api/merchant/draw-results")
public class MerchantDrawController extends MerchantBaseController {

    @Autowired
    private DrawResultService drawResultService;

    @Autowired
    private TimeSlotService timeSlotService;

    @GetMapping
    @Operation(summary = "分页查询抽号记录")
    public Result<IPage<DrawResultVO>> page(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId,
            @Parameter(description = "时段ID") @RequestParam(required = false) Long slotId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        requireMerchantId();
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        DrawQuery query = new DrawQuery();
        query.setPondId(pondId);
        query.setSlotId(slotId);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return Result.success(drawResultService.queryPageVo(query));
    }

    @GetMapping("/missed/{slotId}")
    @Operation(summary = "查询未参与抽号名单")
    public Result<List<MissedDrawVO>> missed(
            @Parameter(description = "时段ID") @PathVariable Long slotId) {
        requireMerchantId();
        TimeSlot slot = timeSlotService.getById(slotId);
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        checkPondOwner(slot.getPondId());
        return Result.success(drawResultService.missedList(slotId));
    }

    @GetMapping("/export")
    @Operation(summary = "导出抽号记录Excel")
    public void export(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId,
            @Parameter(description = "时段ID") @RequestParam(required = false) Long slotId,
            HttpServletResponse response) throws IOException {
        requireMerchantId();
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        DrawQuery query = new DrawQuery();
        query.setPondId(pondId);
        query.setSlotId(slotId);
        drawResultService.exportExcel(query, response);
    }
}
