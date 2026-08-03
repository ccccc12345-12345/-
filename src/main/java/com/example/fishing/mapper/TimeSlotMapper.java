package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.TimeSlot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 时段配置 Mapper
 */
@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlot> {
}
