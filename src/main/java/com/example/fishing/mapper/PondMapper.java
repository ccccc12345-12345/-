package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.Pond;
import org.apache.ibatis.annotations.Mapper;

/**
 * 鱼塘Mapper
 */
@Mapper
public interface PondMapper extends BaseMapper<Pond> {
}
