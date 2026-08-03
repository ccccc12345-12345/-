package com.example.fishing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.FishingSpotDTO;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Pond;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.FishingSpotService;
import com.example.fishing.service.PondService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FishingSpotServiceImpl extends ServiceImpl<FishingSpotMapper, FishingSpot> implements FishingSpotService {

    @Autowired
    private PondService pondService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FishingSpotDTO dto) {
        if (dto.getPondId() == null) {
            dto.setPondId(CurrentUser.getEffectivePondId(null));
        }
        if (dto.getPondId() == null) {
            throw new BusinessException("请选择鱼塘");
        }
        if (lambdaQuery().eq(FishingSpot::getPondId, dto.getPondId()).eq(FishingSpot::getSpotCode, dto.getSpotCode()).count() > 0) {
            throw new BusinessException("当前鱼塘内钓位编号已存在");
        }
        FishingSpot spot = new FishingSpot();
        BeanUtil.copyProperties(dto, spot);
        baseMapper.insert(spot);
        publishSpotBoardChanged(spot.getPondId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FishingSpotDTO dto) {
        FishingSpot exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("钓位不存在");
        }
        checkPondPermission(exist.getPondId());
        Long pondId = dto.getPondId() == null ? exist.getPondId() : dto.getPondId();
        if (!exist.getSpotCode().equals(dto.getSpotCode()) || !exist.getPondId().equals(pondId)) {
            if (lambdaQuery()
                    .eq(FishingSpot::getPondId, pondId)
                    .eq(FishingSpot::getSpotCode, dto.getSpotCode())
                    .ne(FishingSpot::getId, id)
                    .count() > 0) {
                throw new BusinessException("当前鱼塘内钓位编号已存在");
            }
        }
        BeanUtil.copyProperties(dto, exist);
        exist.setPondId(pondId);
        baseMapper.updateById(exist);
        publishSpotBoardChanged(pondId);
    }

    @Override
    public IPage<FishingSpot> pageList(Integer pageNum, Integer pageSize, Long pondId) {
        Long effectivePondId = CurrentUser.getEffectivePondId(pondId);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FishingSpot> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (effectivePondId != null && effectivePondId > 0) {
            wrapper.eq(FishingSpot::getPondId, effectivePondId);
        }
        wrapper.orderByAsc(FishingSpot::getSpotCode).orderByDesc(FishingSpot::getId);
        return page(new Page<>(pageNum, pageSize), wrapper);
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

    private void publishSpotBoardChanged(Long pondId) {
        if (pondId == null) {
            return;
        }
        Pond pond = pondService.getById(pondId);
        if (pond == null || pond.getMerchantId() == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SPOT_BOARD_CHANGED,
                pond.getMerchantId(), pondId, null));
    }
}
