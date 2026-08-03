package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.ShopOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单项 Mapper
 */
@Mapper
public interface ShopOrderItemMapper extends BaseMapper<ShopOrderItem> {

    /**
     * 按订单ID查询订单项
     */
    @Select("SELECT * FROM shop_order_items WHERE order_id = #{orderId} ORDER BY id ASC")
    List<ShopOrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
