package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.TimeSlotDTO;
import com.example.fishing.entity.TimeSlot;

import java.time.LocalDate;
import java.util.List;

/**
 * 时段配置服务
 */
public interface TimeSlotService extends IService<TimeSlot> {

    void create(TimeSlotDTO dto);

    void update(Long id, TimeSlotDTO dto);

    default IPage<TimeSlot> pageList(Integer pageNum, Integer pageSize, Long pondId) {
        return pageList(pageNum, pageSize, pondId, null);
    }

    IPage<TimeSlot> pageList(Integer pageNum, Integer pageSize, Long pondId, LocalDate slotDate);

    /**
     * 为时段列表填充 Redis 实时剩余名额
     */
    void fillRemain(List<TimeSlot> slots);

    /**
     * 删除无有效预约的时段；已有订单的场次需先走变更流程
     */
    void deleteSlot(Long id);
}
