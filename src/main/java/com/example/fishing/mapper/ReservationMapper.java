package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约记录 Mapper
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
