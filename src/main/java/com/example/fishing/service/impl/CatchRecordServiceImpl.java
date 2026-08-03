package com.example.fishing.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.dto.CatchRecordDTO;
import com.example.fishing.entity.CatchRecord;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.RestaurantMenu;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.CatchRecordMapper;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.RestaurantMenuMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.CatchRecordService;
import com.example.fishing.vo.CatchRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 渔获记录服务实现
 */
@Slf4j
@Service
public class CatchRecordServiceImpl extends ServiceImpl<CatchRecordMapper, CatchRecord> implements CatchRecordService {

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RestaurantMenuMapper restaurantMenuMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, CatchRecordDTO dto) {
        if (dto.getWeight() == null || dto.getWeight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("重量必须大于0");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BusinessException("数量必须大于0");
        }
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) {
            throw new BusinessException("鱼塘不存在");
        }
        if (dto.getSpotId() != null) {
            FishingSpot spot = fishingSpotMapper.selectById(dto.getSpotId());
            if (spot == null) {
                throw new BusinessException("钓位不存在");
            }
            if (!dto.getPondId().equals(spot.getPondId())) {
                throw new BusinessException("钓位不属于该鱼塘");
            }
        }

        CatchRecord record = new CatchRecord();
        record.setUserId(userId);
        record.setPondId(dto.getPondId());
        record.setReservationId(dto.getReservationId());
        record.setSpotId(dto.getSpotId());
        record.setFishType(dto.getFishType().trim());
        record.setWeight(dto.getWeight());
        record.setQuantity(dto.getQuantity());
        record.setImageUrl(dto.getImageUrl());
        record.setStatus(Constants.CATCH_PENDING);
        record.setRecyclePrice(null);
        LocalDateTime now = LocalDateTime.now();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        baseMapper.insert(record);

        publishCatchEvent(record);
        return record.getId();
    }

    private void publishCatchEvent(CatchRecord record) {
        if (record == null) {
            return;
        }
        Long merchantId = merchantIdOf(record);
        if (merchantId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.CATCH_CREATED,
                merchantId, record.getPondId(), record.getId(), record.getStatus(), "新渔获提交"));
    }

    @Override
    public List<CatchRecordVO> listByUser(Long userId) {
        List<CatchRecord> list = lambdaQuery()
                .eq(CatchRecord::getUserId, userId)
                .orderByDesc(CatchRecord::getCreateTime)
                .list();
        return convertToVo(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRecycle(Long userId, Long recordId) {
        CatchRecord record = baseMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("渔获记录不存在");
        }
        if (!Constants.CATCH_PENDING.equals(record.getStatus())) {
            throw new BusinessException("当前状态不可申请回收");
        }
        record.setStatus(Constants.CATCH_RECYCLE_REQUESTED);
        record.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(record);
    }

    @Override
    public IPage<CatchRecordVO> queryPage(Long merchantId, Long pondId, String status, Integer pageNum, Integer pageSize) {
        Set<Long> pondIds = queryMerchantPondIds(merchantId, pondId);
        log.info("[渔获查询] merchantId={}, pondId={}, status={}, 商家鱼塘IDs={}", merchantId, pondId, status, pondIds);
        if (pondIds.isEmpty()) {
            log.warn("[渔获查询] 商家没有鱼塘，merchantId={}", merchantId);
            return new Page<>(pageNum, pageSize, 0);
        }
        QueryWrapper<CatchRecord> wrapper = new QueryWrapper<>();
        wrapper.in("pond_id", pondIds);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("create_time");
        long total = baseMapper.selectCount(wrapper);
        log.info("[渔获查询] 符合条件记录数 total={}", total);
        IPage<CatchRecord> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<CatchRecordVO> voList = convertToVo(page.getRecords());
        IPage<CatchRecordVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecycle(Long merchantId, Long recordId, Integer recyclePrice) {
        if (recyclePrice == null || recyclePrice <= 0) {
            throw new BusinessException("回收价格必须大于0");
        }
        CatchRecord record = baseMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("渔获记录不存在");
        }
        Pond pond = pondMapper.selectById(record.getPondId());
        if (pond == null || !merchantId.equals(pond.getMerchantId())) {
            throw new BusinessException("无权操作该渔获记录");
        }
        if (!Constants.CATCH_PENDING.equals(record.getStatus())
                && !Constants.CATCH_RECYCLE_REQUESTED.equals(record.getStatus())) {
            throw new BusinessException("仅待处理或已申请回收的渔获可确认回收");
        }
        record.setStatus(Constants.CATCH_SOLD_RECYCLE);
        record.setRecyclePrice(recyclePrice);
        record.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(record);

        upsertFreshFishMenu(record, recyclePrice);
    }

    private Set<Long> queryMerchantPondIds(Long merchantId, Long pondId) {
        QueryWrapper<Pond> wrapper = new QueryWrapper<>();
        wrapper.eq("merchant_id", merchantId);
        if (pondId != null && pondId > 0) {
            wrapper.eq("id", pondId);
        }
        return pondMapper.selectList(wrapper).stream().map(Pond::getId).collect(Collectors.toSet());
    }

    private void upsertFreshFishMenu(CatchRecord record, Integer recyclePrice) {
        Long merchantId = merchantIdOf(record);
        QueryWrapper<RestaurantMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("pond_id", record.getPondId());
        wrapper.eq("merchant_id", merchantId);
        wrapper.eq("name", record.getFishType());
        wrapper.eq("category", "fresh_fish");
        wrapper.eq("deleted", 0);
        RestaurantMenu menu = restaurantMenuMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (menu == null) {
            menu = new RestaurantMenu();
            menu.setPondId(record.getPondId());
            menu.setMerchantId(merchantIdOf(record));
            menu.setName(record.getFishType());
            menu.setCategory("fresh_fish");
            menu.setPrice(recyclePrice);
            menu.setStock(record.getQuantity());
            menu.setImageUrl(record.getImageUrl());
            menu.setDescription("鲜鱼回收入库");
            menu.setIsSpecial(0);
            menu.setStatus("on");
            menu.setDeleted(0);
            menu.setCreateTime(now);
            menu.setUpdateTime(now);
            restaurantMenuMapper.insert(menu);
        } else {
            int newStock = menu.getStock() == null ? record.getQuantity() : menu.getStock() + record.getQuantity();
            menu.setStock(newStock);
            menu.setUpdateTime(now);
            restaurantMenuMapper.updateById(menu);
        }
    }

    private Long merchantIdOf(CatchRecord record) {
        Pond pond = pondMapper.selectById(record.getPondId());
        return pond == null ? null : pond.getMerchantId();
    }

    private List<CatchRecordVO> convertToVo(List<CatchRecord> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = list.stream().map(CatchRecord::getUserId).collect(Collectors.toSet());
        Set<Long> pondIds = list.stream().map(CatchRecord::getPondId).filter(pid -> pid != null).collect(Collectors.toSet());
        Set<Long> spotIds = list.stream().map(CatchRecord::getSpotId).filter(sid -> sid != null).collect(Collectors.toSet());

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        Map<Long, Pond> pondMap = pondIds.isEmpty() ? new HashMap<>() : pondMapper.selectBatchIds(pondIds)
                .stream().collect(Collectors.toMap(Pond::getId, p -> p));
        Map<Long, FishingSpot> spotMap = spotIds.isEmpty() ? new HashMap<>() : fishingSpotMapper.selectBatchIds(spotIds)
                .stream().collect(Collectors.toMap(FishingSpot::getId, s -> s));

        List<CatchRecordVO> result = new ArrayList<>();
        for (CatchRecord r : list) {
            CatchRecordVO vo = new CatchRecordVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setPondId(r.getPondId());
            vo.setReservationId(r.getReservationId());
            vo.setSpotId(r.getSpotId());
            vo.setFishType(r.getFishType());
            vo.setWeight(r.getWeight());
            vo.setQuantity(r.getQuantity());
            vo.setImageUrl(r.getImageUrl());
            vo.setStatus(r.getStatus());
            vo.setRecyclePrice(r.getRecyclePrice());
            vo.setCreateTime(r.getCreateTime());
            vo.setUpdateTime(r.getUpdateTime());

            SysUser user = userMap.get(r.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }
            Pond pond = pondMap.get(r.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getName());
            }
            FishingSpot spot = spotMap.get(r.getSpotId());
            if (spot != null) {
                vo.setSpotCode(spot.getSpotCode());
            }
            result.add(vo);
        }
        return result;
    }
}
