package com.example.fishing.controller.merchant;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Result;
import com.example.fishing.dto.CheckinWithFeeDTO;
import com.example.fishing.entity.Reservation;
import com.example.fishing.service.CheckinService;
import com.example.fishing.service.ReservationService;
import com.example.fishing.vo.CheckinResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商家核销接口
 */
@Tag(name = "商家核销管理")
@RestController
@RequestMapping("/api/merchant/checkin")
public class MerchantCheckinController extends MerchantBaseController {

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @Operation(summary = "核销预约并设置实际收费")
    public Result<CheckinResultVO> checkin(@Validated @RequestBody CheckinWithFeeDTO dto) {
        requireMerchantId();

        // 先查询校验归属
        CheckinResultVO preVo = checkinService.queryByCode(dto.getCheckinCode());
        Reservation reservation = reservationService.getById(preVo.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        checkPondOwner(reservation.getPondId());

        // 核销并更新金额
        CheckinResultVO vo = checkinService.checkin(dto.getCheckinCode());
        reservationService.updateActualFee(preVo.getReservationId(), dto.getActualFee());
        vo.setActualFee(dto.getActualFee());
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "根据核销码查询预约")
    public Result<CheckinResultVO> query(
            @Parameter(description = "核销码") @RequestParam String checkinCode) {
        requireMerchantId();
        CheckinResultVO vo = checkinService.queryByCode(checkinCode);
        Reservation reservation = reservationService.getById(vo.getReservationId());
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        checkPondOwner(reservation.getPondId());
        return Result.success(vo);
    }
}
