package com.example.fishing.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.DrawQuery;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.DrawResultService;
import com.example.fishing.vo.DrawResultExportVO;
import com.example.fishing.vo.DrawResultVO;
import com.example.fishing.vo.MissedDrawVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 抽号服务实现
 */
@Slf4j
@Service
public class DrawResultServiceImpl extends ServiceImpl<DrawResultMapper, DrawResult> implements DrawResultService {

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 一键抽号
     * 并发控制：
     * 1. Redisson 分布式锁（按 userId）防止同一用户并发点击；
     * 2. 事务内 SELECT ... FOR UPDATE SKIP LOCKED 随机锁定可用钓位，避免多个用户抽到同一钓位；
     * 3. draw_result 唯一索引兜底（reservation_id / slot_id+spot_id）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String draw(Long userId, Long reservationId) {
        RLock lock = redissonClient.getLock(Constants.drawLockKey(userId));
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("系统繁忙，请稍后再试");
            }

            Reservation reservation = reservationMapper.selectById(reservationId);
            if (reservation == null || !reservation.getUserId().equals(userId)) {
                throw new BusinessException("预约记录不存在");
            }
            if (!Constants.RESERVATION_PENDING.equals(reservation.getStatus())) {
                throw new BusinessException("仅待抽号状态可抽号");
            }

            TimeSlot slot = timeSlotMapper.selectById(reservation.getSlotId());
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(slot.getDrawStartTime()) || now.isAfter(slot.getDrawEndTime())) {
                throw new BusinessException("当前不在抽号时间窗口内");
            }

            // 随机抽取可用钓位并加行锁
            FishingSpot spot = fishingSpotMapper.selectRandomAvailableSpot(reservation.getSlotId());
            if (spot == null) {
                throw new BusinessException("暂无可用钓位");
            }

            DrawResult drawResult = new DrawResult();
            drawResult.setReservationId(reservationId);
            drawResult.setUserId(userId);
            drawResult.setSlotId(reservation.getSlotId());
            drawResult.setSpotId(spot.getId());
            drawResult.setDrawTime(now);
            drawResult.setPondId(slot.getPondId());
            baseMapper.insert(drawResult);

            reservation.setStatus(Constants.RESERVATION_DRAWN);
            reservationMapper.updateById(reservation);

            publishDrawEvents(slot, reservation);

            return spot.getSpotCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("抽号中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public IPage<DrawResult> queryPage(DrawQuery query) {
        Long effectivePondId = CurrentUser.getEffectivePondId(query.getPondId());
        return lambdaQuery()
                .eq(query.getUserId() != null, DrawResult::getUserId, query.getUserId())
                .eq(query.getSlotId() != null, DrawResult::getSlotId, query.getSlotId())
                .eq(query.getSpotId() != null, DrawResult::getSpotId, query.getSpotId())
                .eq(effectivePondId != null && effectivePondId > 0, DrawResult::getPondId, effectivePondId)
                .orderByDesc(DrawResult::getDrawTime)
                .page(new Page<>(query.getPageNum(), query.getPageSize()));
    }

    @Override
    public IPage<DrawResultVO> queryPageVo(DrawQuery query) {
        IPage<DrawResult> page = queryPage(query);
        List<DrawResultVO> voList = convertToVoList(page.getRecords());
        IPage<DrawResultVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    private List<DrawResultVO> convertToVoList(List<DrawResult> list) {
        List<DrawResultVO> result = new ArrayList<>();
        if (list.isEmpty()) {
            return result;
        }

        Set<Long> userIds = list.stream().map(DrawResult::getUserId).collect(Collectors.toSet());
        Set<Long> slotIds = list.stream().map(DrawResult::getSlotId).collect(Collectors.toSet());
        Set<Long> spotIds = list.stream().map(DrawResult::getSpotId).collect(Collectors.toSet());
        Set<Long> pondIds = list.stream().map(DrawResult::getPondId).filter(pid -> pid != null).collect(Collectors.toSet());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        Map<Long, TimeSlot> slotMap = timeSlotMapper.selectBatchIds(slotIds)
                .stream().collect(Collectors.toMap(TimeSlot::getId, s -> s));
        Map<Long, FishingSpot> spotMap = fishingSpotMapper.selectBatchIds(spotIds)
                .stream().collect(Collectors.toMap(FishingSpot::getId, s -> s));
        Map<Long, Pond> pondMap = pondIds.isEmpty() ? new HashMap<>() : pondMapper.selectBatchIds(pondIds)
                .stream().collect(Collectors.toMap(Pond::getId, p -> p));

        for (DrawResult d : list) {
            DrawResultVO vo = new DrawResultVO();
            vo.setId(d.getId());
            vo.setReservationId(d.getReservationId());
            vo.setUserId(d.getUserId());
            SysUser user = userMap.get(d.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }
            vo.setSlotId(d.getSlotId());
            TimeSlot slot = slotMap.get(d.getSlotId());
            if (slot != null) {
                vo.setSlotDate(String.valueOf(slot.getSlotDate()));
                vo.setSlotName(slot.getSlotName());
            }
            FishingSpot spot = spotMap.get(d.getSpotId());
            vo.setSpotCode(spot == null ? "" : spot.getSpotCode());
            vo.setDrawTime(d.getDrawTime());
            vo.setPondId(d.getPondId());
            Pond pond = pondMap.get(d.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getName());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<MissedDrawVO> missedList(Long slotId) {
        List<Reservation> reservations = reservationMapper.selectList(
                new QueryWrapper<Reservation>()
                        .eq("slot_id", slotId)
                        .eq("status", Constants.RESERVATION_PENDING));
        if (reservations.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        TimeSlot slot = timeSlotMapper.selectById(slotId);
        List<MissedDrawVO> result = new ArrayList<>();
        for (Reservation r : reservations) {
            MissedDrawVO vo = new MissedDrawVO();
            vo.setReservationId(r.getId());
            vo.setUserId(r.getUserId());
            SysUser user = userMap.get(r.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }
            vo.setSlotId(slotId);
            if (slot != null) {
                vo.setSlotDate(String.valueOf(slot.getSlotDate()));
                vo.setSlotName(slot.getSlotName());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void exportExcel(DrawQuery query, HttpServletResponse response) throws IOException {
        Long effectivePondId = CurrentUser.getEffectivePondId(query.getPondId());
        List<DrawResult> list = lambdaQuery()
                .eq(query.getUserId() != null, DrawResult::getUserId, query.getUserId())
                .eq(query.getSlotId() != null, DrawResult::getSlotId, query.getSlotId())
                .eq(query.getSpotId() != null, DrawResult::getSpotId, query.getSpotId())
                .eq(effectivePondId != null && effectivePondId > 0, DrawResult::getPondId, effectivePondId)
                .orderByDesc(DrawResult::getDrawTime)
                .list();

        List<DrawResultExportVO> voList = convertToVo(list);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("抽号记录", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), DrawResultExportVO.class).sheet("抽号记录").doWrite(voList);
    }

    private List<DrawResultExportVO> convertToVo(List<DrawResult> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> spotIds = list.stream().map(DrawResult::getSpotId).collect(Collectors.toSet());
        Map<Long, FishingSpot> spotMap = fishingSpotMapper.selectBatchIds(spotIds)
                .stream().collect(Collectors.toMap(FishingSpot::getId, s -> s));

        List<DrawResultExportVO> result = new ArrayList<>();
        for (DrawResult d : list) {
            DrawResultExportVO vo = new DrawResultExportVO();
            vo.setId(d.getId());
            vo.setReservationId(d.getReservationId());
            vo.setUserId(d.getUserId());
            vo.setSlotId(d.getSlotId());
            FishingSpot spot = spotMap.get(d.getSpotId());
            vo.setSpotCode(spot == null ? "" : spot.getSpotCode());
            vo.setDrawTime(d.getDrawTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<DrawResultVO> myDraws(Long userId) {
        List<DrawResult> list = lambdaQuery()
                .eq(DrawResult::getUserId, userId)
                .orderByDesc(DrawResult::getDrawTime)
                .list();
        return convertToVoList(list);
    }

    private void publishDrawEvents(TimeSlot slot, Reservation reservation) {
        if (slot == null) {
            return;
        }
        Pond pond = pondMapper.selectById(slot.getPondId());
        Long merchantId = pond == null ? null : pond.getMerchantId();
        if (merchantId == null) {
            return;
        }
        if (reservation != null) {
            eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.RESERVATION_STATUS_CHANGED,
                    merchantId, slot.getPondId(), reservation.getId(),
                    reservation.getStatus(), "预约已抽号"));
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SPOT_BOARD_CHANGED,
                merchantId, slot.getPondId(), null));
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.DASHBOARD_REFRESH,
                merchantId, slot.getPondId(), null));
    }
}
