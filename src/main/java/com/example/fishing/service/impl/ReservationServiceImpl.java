package com.example.fishing.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.ReservationDTO;
import com.example.fishing.dto.ReservationQuery;
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
import com.example.fishing.service.ReservationService;
import com.example.fishing.vo.ReservationExportVO;
import com.example.fishing.vo.ReservationVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long book(Long userId, ReservationDTO dto) {
        Reservation reservation = createReservation(userId, dto, false);
        return reservation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO bookWithSpot(Long userId, ReservationDTO dto) {
        Reservation reservation = createReservation(userId, dto, true);
        return myReservations(userId).stream()
                .filter(item -> reservation.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("预约详情生成失败"));
    }

    private Reservation createReservation(Long userId, ReservationDTO dto, boolean assignSpot) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        Long slotId = dto.getSlotId();
        TimeSlot slot = timeSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException("时段不存在");
        }
        if (!Constants.SLOT_ENABLED.equals(slot.getStatus())) {
            throw new BusinessException("该时段未启用");
        }
        validateBookWindow(slot);

        String remainKey = Constants.slotRemainKey(slotId);
        boolean redisDeducted = deductRemain(slot, remainKey);

        RLock lock = redissonClient.getLock(Constants.reservationLockKey(slotId, userId));
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                rollbackRedisIfNeeded(remainKey, redisDeducted);
                throw new BusinessException("系统繁忙，请稍后再试");
            }

            long existed = lambdaQuery()
                    .eq(Reservation::getUserId, userId)
                    .eq(Reservation::getSlotId, slotId)
                    .in(Reservation::getStatus, activeReservationStatuses())
                    .count();
            if (existed > 0) {
                rollbackRedisIfNeeded(remainKey, redisDeducted);
                throw new BusinessException("您已经预约过该时段");
            }

            long dbCount = countActiveReservations(slotId);
            if (dbCount >= slot.getMaxBookings()) {
                rollbackRedisIfNeeded(remainKey, redisDeducted);
                throw new BusinessException("该时段预约已满");
            }

            FishingSpot spot = null;
            if (assignSpot) {
                spot = fishingSpotMapper.selectRandomAvailableSpot(slotId);
                if (spot == null) {
                    rollbackRedisIfNeeded(remainKey, redisDeducted);
                    throw new BusinessException("当前场次暂无可用钓位");
                }
            }

            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setSlotId(slotId);
            reservation.setPondId(slot.getPondId());
            reservation.setStatus(assignSpot ? Constants.RESERVATION_DRAWN : Constants.RESERVATION_PENDING);
            reservation.setCreateTime(LocalDateTime.now());
            reservation.setCheckinCode(generateCheckinCode());
            baseMapper.insert(reservation);

            if (spot != null) {
                DrawResult drawResult = new DrawResult();
                drawResult.setReservationId(reservation.getId());
                drawResult.setUserId(userId);
                drawResult.setSlotId(slotId);
                drawResult.setSpotId(spot.getId());
                drawResult.setPondId(slot.getPondId());
                drawResult.setDrawTime(LocalDateTime.now());
                drawResultMapper.insert(drawResult);
            }

            publishReservationEvent(reservation,
                    NotificationEventType.RESERVATION_CREATED, "新预约");
            publishDashboardRefresh(reservation.getPondId());

            return reservation;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rollbackRedisIfNeeded(remainKey, redisDeducted);
            throw new BusinessException("预约中断，请重试");
        } catch (RuntimeException e) {
            if (!(e instanceof BusinessException)) {
                rollbackRedisIfNeeded(remainKey, redisDeducted);
            }
            throw e;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private boolean deductRemain(TimeSlot slot, String remainKey) {
        try {
            initRemainIfAbsent(slot, remainKey);
            Long remain = redisTemplate.opsForValue().decrement(remainKey);
            if (remain == null || remain < 0) {
                redisTemplate.opsForValue().increment(remainKey);
                throw new BusinessException("该时段预约已满");
            }
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Throwable e) {
            log.warn("Redis 不可用，预约改用数据库名额校验: {}", e.getMessage());
            return false;
        }
    }

    private void validateBookWindow(TimeSlot slot) {
        if (slot.getSlotDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            throw new BusinessException("时段配置不完整");
        }
        if (slot.getMaxBookings() == null || slot.getMaxBookings() <= 0) {
            throw new BusinessException("时段名额配置不正确");
        }
        LocalDate today = LocalDate.now();
        LocalDate slotDate = slot.getSlotDate();
        if (slotDate.isBefore(today)) {
            throw new BusinessException("该日期已过期");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = LocalDateTime.of(slotDate, slot.getStartTime());
        if (!startAt.isAfter(now)) {
            throw new BusinessException("该时段预约已开始");
        }
        LocalDateTime endAt = LocalDateTime.of(slotDate, slot.getEndTime());
        if (!endAt.isAfter(now)) {
            throw new BusinessException("该时段预约已结束");
        }
        int advanceDays = slot.getAdvanceDays() == null ? 0 : slot.getAdvanceDays();
        if (slotDate.isAfter(today.plusDays(advanceDays))) {
            throw new BusinessException("不在可预约提前天数内");
        }
        if (slot.getDrawEndTime() != null && slot.getDrawEndTime().isBefore(now)) {
            throw new BusinessException("该时段预约已截止");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long reservationId) {
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw new BusinessException("预约记录不存在");
        }
        doCancel(reservation, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long reservationId) {
        adminCancel(reservationId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long reservationId, String reason) {
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        checkPondPermission(reservation.getPondId());
        doCancel(reservation, reason);
    }

    private void doCancel(Reservation reservation, String reason) {
        if (!Constants.RESERVATION_PENDING.equals(reservation.getStatus())
                && !Constants.RESERVATION_DRAWN.equals(reservation.getStatus())) {
            throw new BusinessException("仅待抽号或已分配钓位状态可取消");
        }
        reservation.setStatus(Constants.RESERVATION_CANCELLED);
        reservation.setCancelTime(LocalDateTime.now());
        if (reason != null && !reason.isEmpty()) {
            reservation.setCancelReason(reason);
        }
        baseMapper.updateById(reservation);
        drawResultMapper.delete(new QueryWrapper<DrawResult>().eq("reservation_id", reservation.getId()));

        publishReservationEvent(reservation,
                NotificationEventType.RESERVATION_STATUS_CHANGED, "预约已取消");
        publishDashboardRefresh(reservation.getPondId());
        try {
            redisTemplate.opsForValue().increment(Constants.slotRemainKey(reservation.getSlotId()));
        } catch (Throwable e) {
            log.warn("Redis 不可用，取消时跳过名额恢复: slotId={}, {}", reservation.getSlotId(), e.getMessage());
        }
    }

    @Override
    public IPage<ReservationVO> queryPageVo(ReservationQuery query) {
        IPage<Reservation> page = queryPage(query);
        List<ReservationVO> voList = convertToVo(page.getRecords());
        IPage<ReservationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public List<ReservationVO> myReservations(Long userId) {
        List<Reservation> list = lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime)
                .list();
        return convertToVo(list);
    }

    @Override
    public IPage<Reservation> queryPage(ReservationQuery query) {
        QueryWrapper<Reservation> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    private QueryWrapper<Reservation> buildQueryWrapper(ReservationQuery query) {
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");

        if (query.getUserId() != null) {
            wrapper.eq("user_id", query.getUserId());
        }
        if (query.getSlotId() != null) {
            wrapper.eq("slot_id", query.getSlotId());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq("status", query.getStatus());
        }
        Long effectivePondId = CurrentUser.getEffectivePondId(query.getPondId());
        if (effectivePondId != null && effectivePondId > 0) {
            wrapper.eq("pond_id", effectivePondId);
        } else if (query.getPondIds() != null) {
            if (query.getPondIds().isEmpty()) {
                wrapper.eq("pond_id", -1L);
            } else {
                wrapper.in("pond_id", query.getPondIds());
            }
        }

        if (query.getPhone() != null && !query.getPhone().isEmpty()) {
            List<SysUser> users = sysUserMapper.selectList(new QueryWrapper<SysUser>().like("phone", query.getPhone()));
            if (users.isEmpty()) {
                wrapper.eq("user_id", -1L);
            } else {
                wrapper.in("user_id", users.stream().map(SysUser::getId).collect(Collectors.toList()));
            }
        }

        if ((query.getStartDate() != null && !query.getStartDate().isEmpty())
                || (query.getEndDate() != null && !query.getEndDate().isEmpty())) {
            QueryWrapper<TimeSlot> slotWrapper = new QueryWrapper<>();
            if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
                slotWrapper.ge("slot_date", query.getStartDate());
            }
            if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
                slotWrapper.le("slot_date", query.getEndDate());
            }
            List<TimeSlot> slots = timeSlotMapper.selectList(slotWrapper);
            if (slots.isEmpty()) {
                wrapper.eq("slot_id", -1L);
            } else {
                wrapper.in("slot_id", slots.stream().map(TimeSlot::getId).collect(Collectors.toList()));
            }
        }
        return wrapper;
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

        Map<Long, String> spotCodeMap = new HashMap<>();
        Map<Long, Long> spotIdMap = new HashMap<>();
        Set<Long> reservationIds = list.stream().map(Reservation::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!reservationIds.isEmpty()) {
            List<DrawResult> drawResults = drawResultMapper.selectList(
                    new QueryWrapper<DrawResult>().in("reservation_id", reservationIds));
            Set<Long> spotIds = drawResults.stream().map(DrawResult::getSpotId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, FishingSpot> spotMap = spotIds.isEmpty() ? new HashMap<>() : fishingSpotMapper.selectBatchIds(spotIds)
                    .stream()
                    .filter(spot -> spot != null && spot.getId() != null)
                    .collect(Collectors.toMap(FishingSpot::getId, Function.identity(), (left, right) -> left));
            for (DrawResult d : drawResults) {
                if (d.getReservationId() == null) {
                    continue;
                }
                FishingSpot spot = spotMap.get(d.getSpotId());
                spotCodeMap.put(d.getReservationId(), spot == null ? "" : spot.getSpotCode());
                spotIdMap.put(d.getReservationId(), d.getSpotId());
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
            vo.setPondId(r.getPondId());
            vo.setCheckinCode(r.getCheckinCode());
            vo.setActualFee(r.getActualFee());
            vo.setCheckInTime(r.getCheckInTime());
            vo.setSpotCode(spotCodeMap.get(r.getId()));
            vo.setSpotId(spotIdMap.get(r.getId()));

            SysUser user = userMap.get(r.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }

            TimeSlot slot = slotMap.get(r.getSlotId());
            if (slot != null) {
                vo.setSlotDate(toText(slot.getSlotDate()));
                vo.setSlotName(slot.getSlotName());
                vo.setStartTime(toText(slot.getStartTime()));
                vo.setEndTime(toText(slot.getEndTime()));
                vo.setDrawStartTime(toText(slot.getDrawStartTime()));
                vo.setDrawEndTime(toText(slot.getDrawEndTime()));
            }

            Pond pond = pondMap.get(r.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getName());
            }
            result.add(vo);
        }
        return result;
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void initRemainIfAbsent(TimeSlot slot, String key) {
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(hasKey)) {
                long activeCount = countActiveReservations(slot.getId());
                int remain = Math.max(0, slot.getMaxBookings() - (int) activeCount);
                redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(remain));
            }
        } catch (Throwable e) {
            log.warn("Redis 初始化剩余名额失败: {}", e.getMessage());
        }
    }

    private void rollbackRedisIfNeeded(String remainKey, boolean redisDeducted) {
        if (!redisDeducted) {
            return;
        }
        try {
            redisTemplate.opsForValue().increment(remainKey);
        } catch (Throwable e) {
            log.warn("Redis 名额回滚失败: {}", e.getMessage());
        }
    }

    private long countActiveReservations(Long slotId) {
        if (slotId == null) {
            return 0;
        }
        return lambdaQuery()
                .eq(Reservation::getSlotId, slotId)
                .in(Reservation::getStatus, activeReservationStatuses())
                .count();
    }

    private List<String> activeReservationStatuses() {
        return Arrays.asList(
                Constants.RESERVATION_PENDING,
                Constants.RESERVATION_DRAWN,
                Constants.RESERVATION_CHECKED_IN
        );
    }

    private String generateCheckinCode() {
        for (int i = 0; i < 10; i++) {
            String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
            Long count = lambdaQuery().eq(Reservation::getCheckinCode, code).count();
            if (count == 0) {
                return code;
            }
        }
        throw new BusinessException("核销码生成失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expirePendingReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<TimeSlot> slots = timeSlotMapper.selectList(
                new QueryWrapper<TimeSlot>()
                        .lambda()
                        .eq(TimeSlot::getStatus, Constants.SLOT_ENABLED)
                        .lt(TimeSlot::getDrawEndTime, now)
        );
        for (TimeSlot slot : slots) {
            List<Reservation> pending = lambdaQuery()
                    .eq(Reservation::getSlotId, slot.getId())
                    .eq(Reservation::getStatus, Constants.RESERVATION_PENDING)
                    .list();
            if (pending.isEmpty()) {
                continue;
            }
            for (Reservation reservation : pending) {
                reservation.setStatus(Constants.RESERVATION_EXPIRED);
            }
            updateBatchById(pending);
            for (Reservation reservation : pending) {
                publishReservationEvent(reservation,
                        NotificationEventType.RESERVATION_STATUS_CHANGED, "预约已过期");
            }
            publishDashboardRefresh(slot.getPondId());
            try {
                redisTemplate.opsForValue().increment(Constants.slotRemainKey(slot.getId()), pending.size());
            } catch (Throwable e) {
                log.warn("Redis 不可用，过期预约跳过名额恢复: slotId={}, {}", slot.getId(), e.getMessage());
            }
            log.info("时段{}抽号结束，批量过期{}条预约", slot.getId(), pending.size());
        }
    }

    @Override
    public void exportExcel(ReservationQuery query, HttpServletResponse response) throws IOException {
        List<ReservationVO> list = convertToVo(baseMapper.selectList(buildQueryWrapper(query)));
        List<ReservationExportVO> exportList = new ArrayList<>();
        for (ReservationVO item : list) {
            ReservationExportVO vo = new ReservationExportVO();
            vo.setId(item.getId());
            vo.setUserPhone(item.getUserPhone());
            vo.setUserNickname(item.getUserNickname());
            vo.setSlotDate(item.getSlotDate());
            vo.setSlotName(item.getSlotName());
            vo.setStatus(item.getStatus());
            vo.setSpotCode(item.getSpotCode());
            vo.setCreateTime(toText(item.getCreateTime()));
            exportList.add(vo);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("预约记录", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ReservationExportVO.class).sheet("预约记录").doWrite(exportList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActualFee(Long reservationId, BigDecimal actualFee) {
        if (actualFee == null || actualFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能为负数");
        }
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException("预约记录不存在");
        }
        checkPondPermission(reservation.getPondId());
        reservation.setActualFee(actualFee);
        baseMapper.updateById(reservation);
        publishDashboardRefresh(reservation.getPondId());
    }

    private void checkPondPermission(Long resourcePondId) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx != null && ctx.isNormalAdmin()) {
            Long boundPondId = ctx.getPondId();
            if (boundPondId != null && !boundPondId.equals(resourcePondId)) {
                throw new BusinessException("无权操作其他鱼塘的数据");
            }
        }
    }

    private Long resolveMerchantId(Long pondId) {
        if (pondId == null) {
            return null;
        }
        Pond pond = pondMapper.selectById(pondId);
        return pond == null ? null : pond.getMerchantId();
    }

    private void publishReservationEvent(Reservation reservation,
                                         NotificationEventType type,
                                         String message) {
        if (reservation == null) {
            return;
        }
        Long merchantId = resolveMerchantId(reservation.getPondId());
        if (merchantId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(type, merchantId, reservation.getPondId(),
                reservation.getId(), reservation.getStatus(), message));
    }

    private void publishDashboardRefresh(Long pondId) {
        Long merchantId = resolveMerchantId(pondId);
        if (merchantId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.DASHBOARD_REFRESH,
                merchantId, pondId, null));
    }
}
