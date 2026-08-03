package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fishing.common.Constants;
import com.example.fishing.common.Result;
import com.example.fishing.entity.DrawResult;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.Reservation;
import com.example.fishing.entity.SysUser;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.mapper.DrawResultMapper;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.mapper.TimeSlotMapper;
import com.example.fishing.service.ReservationService;
import com.example.fishing.vo.DashboardStatsVO;
import com.example.fishing.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商家工作台接口
 */
@Tag(name = "商家工作台")
@RestController
@RequestMapping("/api/merchant/dashboard")
public class MerchantDashboardController extends MerchantBaseController {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DrawResultMapper drawResultMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/stats")
    @Operation(summary = "商家工作台统计")
    public Result<DashboardStatsVO> stats(
            @Parameter(description = "鱼塘ID，不传则统计所有鱼塘") @RequestParam(required = false) Long pondId) {
        Long merchantId = requireMerchantId();

        List<Long> pondIds;
        if (pondId != null) {
            checkPondOwner(pondId);
            pondIds = Collections.singletonList(pondId);
        } else {
            pondIds = pondMapper.selectList(
                    new QueryWrapper<Pond>().eq("merchant_id", merchantId))
                    .stream().map(Pond::getId).collect(Collectors.toList());
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        DashboardStatsVO vo = new DashboardStatsVO();

        // 今日收入：已核销且核销时间在今日
        QueryWrapper<Reservation> incomeWrapper = new QueryWrapper<>();
        incomeWrapper.eq("status", Constants.RESERVATION_CHECKED_IN)
                .between("check_in_time", todayStart, todayEnd);
        if (!pondIds.isEmpty()) {
            incomeWrapper.in("pond_id", pondIds);
        } else {
            incomeWrapper.eq("pond_id", -1L);
        }
        List<Reservation> incomeList = reservationMapper.selectList(incomeWrapper);
        BigDecimal todayIncome = incomeList.stream()
                .map(r -> r.getActualFee() != null ? r.getActualFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayIncome(todayIncome);

        // 今日预约数：创建时间在今日
        QueryWrapper<Reservation> reservationWrapper = new QueryWrapper<>();
        reservationWrapper.between("create_time", todayStart, todayEnd);
        if (!pondIds.isEmpty()) {
            reservationWrapper.in("pond_id", pondIds);
        } else {
            reservationWrapper.eq("pond_id", -1L);
        }
        Long todayReservationCount = (long) reservationMapper.selectCount(reservationWrapper);
        vo.setTodayReservationCount(todayReservationCount);

        // 今日核销数
        QueryWrapper<Reservation> checkinWrapper = new QueryWrapper<>();
        checkinWrapper.eq("status", Constants.RESERVATION_CHECKED_IN)
                .between("check_in_time", todayStart, todayEnd);
        if (!pondIds.isEmpty()) {
            checkinWrapper.in("pond_id", pondIds);
        } else {
            checkinWrapper.eq("pond_id", -1L);
        }
        Long todayCheckinCount = (long) reservationMapper.selectCount(checkinWrapper);
        vo.setTodayCheckinCount(todayCheckinCount);

        // 上座率
        if (todayReservationCount == 0) {
            vo.setOccupancyRate(BigDecimal.ZERO);
        } else {
            BigDecimal rate = BigDecimal.valueOf(todayCheckinCount * 100)
                    .divide(BigDecimal.valueOf(todayReservationCount), 2, RoundingMode.HALF_UP);
            vo.setOccupancyRate(rate);
        }

        // 最近5条预约
        QueryWrapper<Reservation> recentWrapper = new QueryWrapper<>();
        if (!pondIds.isEmpty()) {
            recentWrapper.in("pond_id", pondIds);
        } else {
            recentWrapper.eq("pond_id", -1L);
        }
        recentWrapper.orderByDesc("create_time").last("LIMIT 5");
        List<Reservation> recentList = reservationMapper.selectList(recentWrapper);
        vo.setRecentReservations(convertToVo(recentList));

        return Result.success(vo);
    }

    private List<ReservationVO> convertToVo(List<Reservation> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = list.stream().map(Reservation::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> slotIds = list.stream().map(Reservation::getSlotId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> pondIds = list.stream().map(Reservation::getPondId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>() : sysUserMapper.selectBatchIds(userIds)
                .stream()
                .filter(user -> user != null && user.getId() != null)
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Map<Long, TimeSlot> slotMap = slotIds.isEmpty() ? new HashMap<>() : timeSlotMapper.selectBatchIds(slotIds)
                .stream()
                .filter(slot -> slot != null && slot.getId() != null)
                .collect(Collectors.toMap(TimeSlot::getId, Function.identity(), (left, right) -> left));
        Map<Long, Pond> pondMap = pondIds.isEmpty() ? new HashMap<>() : pondMapper.selectBatchIds(pondIds)
                .stream()
                .filter(pond -> pond != null && pond.getId() != null)
                .collect(Collectors.toMap(Pond::getId, Function.identity(), (left, right) -> left));

        Set<Long> drawnReservationIds = list.stream()
                .filter(r -> r.getId() != null && Constants.RESERVATION_DRAWN.equals(r.getStatus()))
                .map(Reservation::getId)
                .collect(Collectors.toSet());
        Map<Long, String> spotCodeMap = new HashMap<>();
        if (!drawnReservationIds.isEmpty()) {
            List<DrawResult> drawResults = drawResultMapper.selectList(
                    new QueryWrapper<DrawResult>().in("reservation_id", drawnReservationIds));
            Set<Long> spotIds = drawResults.stream().map(DrawResult::getSpotId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, FishingSpot> spotMap = spotIds.isEmpty() ? new HashMap<>() : fishingSpotMapper.selectBatchIds(spotIds)
                    .stream()
                    .filter(spot -> spot != null && spot.getId() != null)
                    .collect(Collectors.toMap(FishingSpot::getId, Function.identity(), (left, right) -> left));
            for (DrawResult d : drawResults) {
                FishingSpot spot = spotMap.get(d.getSpotId());
                if (d.getReservationId() != null) {
                    spotCodeMap.put(d.getReservationId(), spot == null ? "" : spot.getSpotCode());
                }
            }
        }

        List<ReservationVO> result = new ArrayList<>();
        for (Reservation r : list) {
            ReservationVO vo = new ReservationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setSlotId(r.getSlotId());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setCancelTime(r.getCancelTime());
            vo.setCancelReason(r.getCancelReason());

            SysUser user = userMap.get(r.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }

            TimeSlot slot = slotMap.get(r.getSlotId());
            if (slot != null) {
                vo.setSlotDate(String.valueOf(slot.getSlotDate()));
                vo.setSlotName(slot.getSlotName());
                vo.setStartTime(String.valueOf(slot.getStartTime()));
                vo.setEndTime(String.valueOf(slot.getEndTime()));
                vo.setDrawStartTime(String.valueOf(slot.getDrawStartTime()));
                vo.setDrawEndTime(String.valueOf(slot.getDrawEndTime()));
            }

            Pond pond = pondMap.get(r.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getName());
            }
            vo.setPondId(r.getPondId());
            vo.setCheckinCode(r.getCheckinCode());
            vo.setActualFee(r.getActualFee());
            vo.setCheckInTime(r.getCheckInTime());
            vo.setSpotCode(spotCodeMap.getOrDefault(r.getId(), null));
            result.add(vo);
        }
        return result;
    }
}
