package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fishing.common.Constants;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.RevenueQuery;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.Reservation;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.TimeSlotMapper;
import com.example.fishing.service.RevenueService;
import com.example.fishing.vo.RevenueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收益统计服务实现
 */
@Service
public class RevenueServiceImpl implements RevenueService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Autowired
    private PondMapper pondMapper;

    @Override
    public Map<String, BigDecimal> summary(Long pondId) {
        Long effectivePondId = CurrentUser.getEffectivePondId(pondId);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStart = monday.atStartOfDay();

        LocalDate firstDayOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("today", sumIncome(todayStart, todayEnd, effectivePondId));
        result.put("week", sumIncome(weekStart, todayEnd, effectivePondId));
        result.put("month", sumIncome(monthStart, todayEnd, effectivePondId));
        return result;
    }

    private BigDecimal sumIncome(LocalDateTime start, LocalDateTime end, Long pondId) {
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.eq("status", Constants.RESERVATION_CHECKED_IN);
        wrapper.between("check_in_time", start, end);
        if (pondId != null && pondId > 0) {
            wrapper.eq("pond_id", pondId);
        }
        List<Reservation> list = reservationMapper.selectList(wrapper);
        return list.stream()
                .map(r -> r.getActualFee() != null ? r.getActualFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<RevenueVO> list(RevenueQuery query) {
        Long effectivePondId = CurrentUser.getEffectivePondId(query.getPondId());
        // 查询时段
        QueryWrapper<TimeSlot> slotWrapper = new QueryWrapper<>();
        if (effectivePondId != null && effectivePondId > 0) {
            slotWrapper.eq("pond_id", effectivePondId);
        }
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            slotWrapper.ge("slot_date", query.getStartDate());
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            slotWrapper.le("slot_date", query.getEndDate());
        }
        List<TimeSlot> slots = timeSlotMapper.selectList(slotWrapper);
        if (slots.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> slotIds = slots.stream().map(TimeSlot::getId).collect(Collectors.toSet());
        Map<Long, TimeSlot> slotMap = slots.stream().collect(Collectors.toMap(TimeSlot::getId, s -> s));

        // 查询关联预约
        QueryWrapper<Reservation> resWrapper = new QueryWrapper<>();
        resWrapper.in("slot_id", slotIds);
        List<Reservation> reservations = reservationMapper.selectList(resWrapper);

        Map<Long, String> pondNameMap = pondMapper.selectList(null)
                .stream().collect(Collectors.toMap(Pond::getId, Pond::getName));

        // 按时段分组统计
        Map<Long, List<Reservation>> group = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getSlotId));

        List<RevenueVO> result = new ArrayList<>();
        for (TimeSlot slot : slots) {
            List<Reservation> list = group.getOrDefault(slot.getId(), new ArrayList<>());
            long total = list.size();
            long checkin = list.stream()
                    .filter(r -> Constants.RESERVATION_CHECKED_IN.equals(r.getStatus()))
                    .count();
            BigDecimal income = list.stream()
                    .filter(r -> Constants.RESERVATION_CHECKED_IN.equals(r.getStatus()))
                    .map(r -> r.getActualFee() != null ? r.getActualFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            RevenueVO vo = new RevenueVO();
            vo.setSlotDate(String.valueOf(slot.getSlotDate()));
            vo.setPondId(slot.getPondId());
            vo.setPondName(pondNameMap.getOrDefault(slot.getPondId(), ""));
            vo.setSlotId(slot.getId());
            vo.setSlotName(slot.getSlotName());
            vo.setTotalCount(total);
            vo.setCheckinCount(checkin);
            vo.setTotalIncome(income);
            BigDecimal rate = total == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(checkin * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            vo.setOccupancyRate(rate);
            result.add(vo);
        }

        // 按日期降序
        result.sort((a, b) -> b.getSlotDate().compareTo(a.getSlotDate()));
        return result;
    }

    @Override
    public void exportExcel(RevenueQuery query, HttpServletResponse response) throws IOException {
        List<RevenueVO> list = list(query);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("收益统计", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), RevenueVO.class).sheet("收益统计").doWrite(list);
    }
}
