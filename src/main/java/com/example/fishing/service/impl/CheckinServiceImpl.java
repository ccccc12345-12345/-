package com.example.fishing.service.impl;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.entity.*;
import com.example.fishing.mapper.*;
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.CheckinService;
import com.example.fishing.vo.CheckinResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 核销服务实现
 */
@Service
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Autowired
    private DrawResultMapper drawResultMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO checkin(String checkinCode) {
        Reservation reservation = validateAndGet(checkinCode);
        TimeSlot slot = timeSlotMapper.selectById(reservation.getSlotId());
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        // 有效期内：预约日期当天或之前不允许提前核销，之后不允许补核销（可按业务调整）
        if (slot.getSlotDate().isAfter(LocalDate.now())) {
            throw new BusinessException("未到预约日期，无法核销");
        }

        // 自动填充票价
        if (reservation.getActualFee() == null && slot.getDefaultPrice() != null) {
            reservation.setActualFee(slot.getDefaultPrice());
        }

        reservation.setStatus(Constants.RESERVATION_CHECKED_IN);
        reservation.setCheckInTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        publishCheckinEvent(reservation, slot);

        return buildVo(reservation, slot);
    }

    @Override
    public CheckinResultVO queryByCode(String checkinCode) {
        Reservation reservation = validateAndGet(checkinCode);
        TimeSlot slot = timeSlotMapper.selectById(reservation.getSlotId());
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        return buildVo(reservation, slot);
    }

    private Reservation validateAndGet(String checkinCode) {
        if (checkinCode == null || checkinCode.trim().isEmpty()) {
            throw new BusinessException("核销码不能为空");
        }
        Reservation reservation = reservationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Reservation>()
                        .eq("checkin_code", checkinCode.trim()));
        if (reservation == null) {
            throw new BusinessException("无效的核销码");
        }
        if (Constants.RESERVATION_CHECKED_IN.equals(reservation.getStatus())) {
            throw new BusinessException("该预约已核销");
        }
        if (!Constants.RESERVATION_DRAWN.equals(reservation.getStatus())) {
            throw new BusinessException("仅已抽号状态可核销");
        }
        return reservation;
    }

    private CheckinResultVO buildVo(Reservation reservation, TimeSlot slot) {
        CheckinResultVO vo = new CheckinResultVO();
        vo.setReservationId(reservation.getId());
        vo.setSlotDate(String.valueOf(slot.getSlotDate()));
        vo.setSlotName(slot.getSlotName());
        vo.setActualFee(reservation.getActualFee());
        vo.setCheckInTime(reservation.getCheckInTime());
        vo.setStatus(reservation.getStatus());

        SysUser user = sysUserMapper.selectById(reservation.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
        }

        DrawResult drawResult = drawResultMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DrawResult>()
                        .eq("reservation_id", reservation.getId()));
        if (drawResult != null) {
            FishingSpot spot = fishingSpotMapper.selectById(drawResult.getSpotId());
            if (spot != null) {
                vo.setSpotCode(spot.getSpotCode());
            }
        }

        Pond pond = pondMapper.selectById(slot.getPondId());
        if (pond != null) {
            vo.setPondName(pond.getName());
        }

        return vo;
    }

    private void publishCheckinEvent(Reservation reservation, TimeSlot slot) {
        if (reservation == null || slot == null) {
            return;
        }
        Pond pond = pondMapper.selectById(slot.getPondId());
        Long merchantId = pond == null ? null : pond.getMerchantId();
        if (merchantId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.RESERVATION_CHECKED_IN,
                merchantId, slot.getPondId(), reservation.getId(),
                reservation.getStatus(), "预约已核销"));
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.DASHBOARD_REFRESH,
                merchantId, slot.getPondId(), null));
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SPOT_BOARD_CHANGED,
                merchantId, slot.getPondId(), null));
    }
}
