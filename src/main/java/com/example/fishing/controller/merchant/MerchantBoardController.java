package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fishing.common.Constants;
import com.example.fishing.common.Result;
import com.example.fishing.entity.DrawResult;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Reservation;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.DrawResultMapper;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.vo.SpotBoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "商家钓位看板")
@RestController
@RequestMapping("/api/merchant/pond-board")
public class MerchantBoardController extends MerchantBaseController {

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private DrawResultMapper drawResultMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @GetMapping
    @Operation(summary = "钓位看板数据")
    public Result<List<SpotBoardVO>> board(
            @Parameter(description = "鱼塘ID") @RequestParam Long pondId,
            @Parameter(description = "日期") @RequestParam(required = false) String date,
            @Parameter(description = "场次ID") @RequestParam Long slotId) {
        requireMerchantId();
        checkPondOwner(pondId);

        List<FishingSpot> spots = fishingSpotMapper.selectList(
                new QueryWrapper<FishingSpot>().eq("pond_id", pondId).orderByAsc("spot_code"));
        if (spots.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<String> activeStatus = new ArrayList<>();
        activeStatus.add(Constants.RESERVATION_PENDING);
        activeStatus.add(Constants.RESERVATION_DRAWN);
        activeStatus.add(Constants.RESERVATION_CHECKED_IN);
        List<Reservation> reservations = reservationMapper.selectList(
                new QueryWrapper<Reservation>()
                        .eq("slot_id", slotId)
                        .in("status", activeStatus));

        List<DrawResult> drawResults = drawResultMapper.selectList(
                new QueryWrapper<DrawResult>().eq("slot_id", slotId));
        Map<Long, DrawResult> reservationDrawMap = drawResults.stream()
                .collect(Collectors.toMap(DrawResult::getReservationId, d -> d, (a, b) -> a));

        Map<Long, Reservation> spotReservationMap = new HashMap<>();
        for (Reservation reservation : reservations) {
            DrawResult drawResult = reservationDrawMap.get(reservation.getId());
            if (drawResult != null && drawResult.getSpotId() != null) {
                spotReservationMap.put(drawResult.getSpotId(), reservation);
            }
        }

        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>() : sysUserMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<SpotBoardVO> result = new ArrayList<>();
        for (FishingSpot spot : spots) {
            SpotBoardVO vo = new SpotBoardVO();
            vo.setSpotId(spot.getId());
            vo.setSpotCode(spot.getSpotCode());
            vo.setSpotStatus(spot.getStatus());
            vo.setCoordinateX(spot.getCoordinateX());
            vo.setCoordinateY(spot.getCoordinateY());

            if (Constants.SPOT_DISABLED.equals(spot.getStatus())) {
                vo.setStatus("disabled");
            } else if (Constants.SPOT_MAINTENANCE.equals(spot.getStatus())) {
                vo.setStatus("maintenance");
            } else {
                Reservation reservation = spotReservationMap.get(spot.getId());
                if (reservation != null) {
                    vo.setReservationId(reservation.getId());
                    vo.setReservationStatus(reservation.getStatus());
                    vo.setStatus(Constants.RESERVATION_CHECKED_IN.equals(reservation.getStatus()) ? "using" : "reserved");
                    SysUser user = userMap.get(reservation.getUserId());
                    if (user != null) {
                        vo.setUserNickname(user.getNickname());
                        vo.setUserPhone(maskPhone(user.getPhone()));
                    }
                } else {
                    vo.setStatus("free");
                }
            }
            result.add(vo);
        }
        return Result.success(result);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(phone.length() - 4);
    }
}
