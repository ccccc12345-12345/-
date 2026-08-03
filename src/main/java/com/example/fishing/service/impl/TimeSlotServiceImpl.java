package com.example.fishing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.TimeSlotDTO;
import com.example.fishing.entity.Reservation;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.TimeSlotMapper;
import com.example.fishing.service.TimeSlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 时段配置服务实现
 */
@Service
public class TimeSlotServiceImpl extends ServiceImpl<TimeSlotMapper, TimeSlot> implements TimeSlotService {

    private static final Logger log = LoggerFactory.getLogger(TimeSlotServiceImpl.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ReservationMapper reservationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(TimeSlotDTO dto) {
        normalizeAndValidate(dto, null);
        TimeSlot slot = new TimeSlot();
        BeanUtil.copyProperties(dto, slot);
        if (slot.getPondId() == null) {
            slot.setPondId(CurrentUser.getEffectivePondId(null));
        }
        baseMapper.insert(slot);
        // 启用时段时预热 Redis 名额（Redis 不可用时降级为静默跳过）
        if (Constants.SLOT_ENABLED.equals(slot.getStatus())) {
            warmRemainSafe(slot);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TimeSlotDTO dto) {
        TimeSlot exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("时段不存在");
        }
        checkPondPermission(exist.getPondId());
        normalizeAndValidate(dto, id);
        checkPondPermission(dto.getPondId());
        protectLockedFields(exist, dto);
        BeanUtil.copyProperties(dto, exist);
        baseMapper.updateById(exist);
        // 如果处于启用状态，重新写入 Redis 名额（覆盖原有值，管理端修改后重新发布）
        if (Constants.SLOT_ENABLED.equals(exist.getStatus())) {
            warmRemainSafe(exist);
        } else {
            deleteRemainSafe(id);
        }
    }

    @Override
    public IPage<TimeSlot> pageList(Integer pageNum, Integer pageSize, Long pondId, LocalDate slotDate) {
        Long effectivePondId = CurrentUser.getEffectivePondId(pondId);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TimeSlot> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (effectivePondId != null && effectivePondId > 0) {
            wrapper.eq(TimeSlot::getPondId, effectivePondId);
        }
        if (slotDate != null) {
            wrapper.eq(TimeSlot::getSlotDate, slotDate);
        }
        wrapper.orderByDesc(TimeSlot::getSlotDate).orderByAsc(TimeSlot::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void fillRemain(List<TimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (TimeSlot slot : slots) {
            try {
                String key = Constants.slotRemainKey(slot.getId());
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    try {
                        slot.setRemain(Math.max(0, Math.min(Integer.parseInt(value), calculateDbRemain(slot))));
                    } catch (NumberFormatException ignored) {
                        slot.setRemain(calculateDbRemain(slot));
                    }
                } else {
                    slot.setRemain(calculateDbRemain(slot));
                }
            } catch (Throwable e) {
                log.warn("Redis 不可用，使用默认剩余名额: {}", e.getMessage());
                slot.setRemain(calculateDbRemain(slot));
            }
        }
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

    /**
     * Redis 不可用时静默降级
     */
    private void warmRemainSafe(TimeSlot slot) {
        try {
            warmRemain(slot);
        } catch (Throwable e) {
            log.warn("Redis 不可用，跳过预热时段名额: slotId={}, {}", slot.getId(), e.getMessage());
        }
    }

    private void deleteRemainSafe(Long slotId) {
        try {
            redisTemplate.delete(Constants.slotRemainKey(slotId));
        } catch (Throwable e) {
            log.warn("Redis 不可用，跳过删除名额缓存: slotId={}, {}", slotId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlot(Long id) {
        TimeSlot slot = baseMapper.selectById(id);
        if (slot == null) {
            return;
        }
        checkPondPermission(slot.getPondId());
        if (countActiveReservations(id) > 0) {
            throw new BusinessException("该场次已有订单，不能直接删除，请先停用并走变更流程");
        }

        // 1. 物理删除时段
        baseMapper.deleteById(id);
        deleteRemainSafe(id);

        log.info("删除时段 slotId={}", id);
    }

    /**
     * 将时段剩余名额写入 Redis，设置合理过期时间，防止缓存长期占用
     */
    private void warmRemain(TimeSlot slot) {
        String key = Constants.slotRemainKey(slot.getId());
        // 缓存过期时间：到抽号结束后再保留 2 小时
        LocalDateTime expireAt = slot.getDrawEndTime() == null
                ? LocalDateTime.of(slot.getSlotDate(), slot.getEndTime()).plusHours(2)
                : slot.getDrawEndTime().plusHours(2);
        long seconds = Duration.between(LocalDateTime.now(), expireAt).getSeconds();
        if (seconds <= 0) {
            return;
        }
        long activeCount = countActiveReservations(slot.getId());
        int remain = Math.max(0, slot.getMaxBookings() - (int) activeCount);
        redisTemplate.opsForValue().set(key, String.valueOf(remain), seconds, TimeUnit.SECONDS);
    }

    private void normalizeAndValidate(TimeSlotDTO dto, Long ignoreId) {
        if (dto == null) {
            throw new BusinessException("时段参数不能为空");
        }
        if (dto.getPondId() == null) {
            dto.setPondId(CurrentUser.getEffectivePondId(null));
        }
        if (dto.getPondId() == null || dto.getPondId() <= 0) {
            throw new BusinessException("鱼塘不能为空");
        }
        if (dto.getSlotDate() == null) {
            throw new BusinessException("场次日期不能为空");
        }
        if (dto.getSlotName() == null || dto.getSlotName().trim().isEmpty()) {
            throw new BusinessException("时段名称不能为空");
        }
        dto.setSlotName(dto.getSlotName().trim());
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException("开始和结束时间不能为空");
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getMaxBookings() == null || dto.getMaxBookings() <= 0) {
            throw new BusinessException("最大预约人数必须大于 0");
        }
        if (dto.getAdvanceDays() == null || dto.getAdvanceDays() < 0) {
            throw new BusinessException("预约开放天数不能小于 0");
        }
        if (dto.getDrawStartTime() == null || dto.getDrawEndTime() == null) {
            throw new BusinessException("抽号开始和结束时间不能为空");
        }
        if (dto.getDrawStartTime().isAfter(dto.getDrawEndTime())) {
            throw new BusinessException("抽号结束时间必须晚于开始时间");
        }
        LocalDateTime startAt = LocalDateTime.of(dto.getSlotDate(), dto.getStartTime());
        if (dto.getDrawEndTime().isAfter(startAt)) {
            throw new BusinessException("抽号结束时间不能晚于开场时间");
        }
        if (!Constants.SLOT_ENABLED.equals(dto.getStatus()) && !Constants.SLOT_DISABLED.equals(dto.getStatus())) {
            throw new BusinessException("场次状态不正确");
        }
        if (dto.getDefaultPrice() != null && dto.getDefaultPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("价格不能小于 0");
        }
        if (Constants.SLOT_ENABLED.equals(dto.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            if (!startAt.isAfter(now)) {
                throw new BusinessException("不能发布已经开始或结束的场次");
            }
            if (!dto.getDrawEndTime().isAfter(now)) {
                throw new BusinessException("不能发布预约截止时间已过的场次");
            }
        }
        ensureNoDuplicateSlot(dto, ignoreId);
    }

    private void ensureNoDuplicateSlot(TimeSlotDTO dto, Long ignoreId) {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getPondId, dto.getPondId())
                .eq(TimeSlot::getSlotDate, dto.getSlotDate())
                .eq(TimeSlot::getSlotName, dto.getSlotName())
                .eq(TimeSlot::getStartTime, dto.getStartTime())
                .eq(TimeSlot::getEndTime, dto.getEndTime());
        if (ignoreId != null) {
            wrapper.ne(TimeSlot::getId, ignoreId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该鱼塘当天已存在相同预约场次");
        }
    }

    private void protectLockedFields(TimeSlot exist, TimeSlotDTO dto) {
        if (countActiveReservations(exist.getId()) <= 0) {
            return;
        }
        boolean changed = !Objects.equals(exist.getPondId(), dto.getPondId())
                || !Objects.equals(exist.getSlotDate(), dto.getSlotDate())
                || !Objects.equals(exist.getSlotName(), dto.getSlotName())
                || !Objects.equals(exist.getStartTime(), dto.getStartTime())
                || !Objects.equals(exist.getEndTime(), dto.getEndTime())
                || !Objects.equals(exist.getMaxBookings(), dto.getMaxBookings())
                || priceChanged(exist.getDefaultPrice(), dto.getDefaultPrice());
        if (changed) {
            throw new BusinessException("该场次已有订单，请先走变更流程");
        }
    }

    private boolean priceChanged(BigDecimal left, BigDecimal right) {
        BigDecimal a = left == null ? BigDecimal.ZERO : left;
        BigDecimal b = right == null ? BigDecimal.ZERO : right;
        return a.compareTo(b) != 0;
    }

    private long countActiveReservations(Long slotId) {
        if (slotId == null) {
            return 0;
        }
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getSlotId, slotId)
                .in(Reservation::getStatus, activeReservationStatuses());
        Long count = reservationMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    private int calculateDbRemain(TimeSlot slot) {
        if (slot == null || slot.getMaxBookings() == null) {
            return 0;
        }
        return Math.max(0, slot.getMaxBookings() - (int) countActiveReservations(slot.getId()));
    }

    private List<String> activeReservationStatuses() {
        return Arrays.asList(
                Constants.RESERVATION_PENDING,
                Constants.RESERVATION_DRAWN,
                Constants.RESERVATION_CHECKED_IN
        );
    }
}
