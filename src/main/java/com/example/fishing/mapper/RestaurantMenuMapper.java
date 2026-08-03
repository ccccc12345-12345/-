package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.RestaurantMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 餐厅菜单 Mapper
 */
@Mapper
public interface RestaurantMenuMapper extends BaseMapper<RestaurantMenu> {
}
