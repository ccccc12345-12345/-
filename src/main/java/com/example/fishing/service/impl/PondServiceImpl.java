package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fishing.common.BusinessException;
import com.example.fishing.entity.Pond;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.TimeSlotMapper;
import com.example.fishing.service.PondService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 鱼塘服务实现
 */
@Service
public class PondServiceImpl implements PondService {

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Override
    public List<Pond> listAll() {
        return pondMapper.selectList(null);
    }

    @Override
    public List<Pond> listByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Pond> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pond::getMerchantId, merchantId);
        wrapper.orderByDesc(Pond::getCreateTime);
        return pondMapper.selectList(wrapper);
    }

    @Override
    public Pond getById(Long id) {
        return pondMapper.selectById(id);
    }

    @Override
    public void create(Pond pond) {
        if (pond.getName() == null || pond.getName().trim().isEmpty()) {
            throw new BusinessException("鱼塘名称不能为空");
        }
        pond.setName(pond.getName().trim());
        LocalDateTime now = LocalDateTime.now();
        pond.setCreateTime(now);
        pond.setUpdateTime(now);
        if (pond.getStatus() == null) {
            pond.setStatus(1);
        }
        pondMapper.insert(pond);
    }

    @Override
    public void update(Long id, Pond pond) {
        Pond exist = pondMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("鱼塘不存在");
        }
        if (pond.getName() == null || pond.getName().trim().isEmpty()) {
            throw new BusinessException("鱼塘名称不能为空");
        }
        exist.setName(pond.getName().trim());
        exist.setAddress(pond.getAddress());
        exist.setPhone(pond.getPhone());
        exist.setStatus(pond.getStatus());
        exist.setUpdateTime(LocalDateTime.now());
        pondMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        Pond exist = pondMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("鱼塘不存在");
        }
        // 校验是否有关联时段
        LambdaQueryWrapper<com.example.fishing.entity.TimeSlot> slotWrapper = new LambdaQueryWrapper<>();
        slotWrapper.eq(com.example.fishing.entity.TimeSlot::getPondId, id);
        if (timeSlotMapper.selectCount(slotWrapper) > 0) {
            throw new BusinessException("该鱼塘下存在时段配置，无法删除");
        }
        // 校验是否有关联预约
        LambdaQueryWrapper<com.example.fishing.entity.Reservation> resWrapper = new LambdaQueryWrapper<>();
        resWrapper.eq(com.example.fishing.entity.Reservation::getPondId, id);
        if (reservationMapper.selectCount(resWrapper) > 0) {
            throw new BusinessException("该鱼塘下存在预约记录，无法删除");
        }
        pondMapper.deleteById(id);
    }

    @Override
    public void checkMerchantOwner(Long pondId, Long merchantId) {
        if (pondId == null || merchantId == null) {
            throw new BusinessException("参数错误");
        }
        Pond pond = pondMapper.selectById(pondId);
        if (pond == null) {
            throw new BusinessException("鱼塘不存在");
        }
        if (!merchantId.equals(pond.getMerchantId())) {
            throw new BusinessException("无权操作该鱼塘");
        }
    }
}
