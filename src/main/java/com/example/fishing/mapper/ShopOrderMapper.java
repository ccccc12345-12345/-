package com.example.fishing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fishing.entity.ShopOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface ShopOrderMapper extends BaseMapper<ShopOrder> {

    /**
     * 查询商家餐厅订单列表（含用户信息）
     */
    @Select("<script>" +
            "SELECT o.*, u.nickname as user_nickname, u.phone as user_phone, s.spot_code as spot_code " +
            "FROM shop_orders o " +
            "LEFT JOIN sys_user u ON o.user_id = u.id " +
            "LEFT JOIN fishing_spot s ON o.spot_id = s.id " +
            "WHERE o.deleted = 0 AND o.order_type = 'restaurant' " +
            "<if test='merchantId != null'> AND o.merchant_id = #{merchantId} </if>" +
            "<if test='pondId != null'> AND o.pond_id = #{pondId} </if>" +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "ORDER BY o.create_time DESC" +
            "</script>")
    List<com.example.fishing.vo.RestaurantOrderVO> selectMerchantRestaurantOrders(@Param("merchantId") Long merchantId,
                                                                                   @Param("pondId") Long pondId,
                                                                                   @Param("status") String status);

    /**
     * 查询当前用户的餐厅订单列表（含鱼塘和钓位信息）
     */
    @Select("SELECT o.*, p.name as pond_name, s.spot_code as spot_code " +
            "FROM shop_orders o " +
            "LEFT JOIN pond p ON o.pond_id = p.id " +
            "LEFT JOIN fishing_spot s ON o.spot_id = s.id " +
            "WHERE o.deleted = 0 AND o.order_type = 'restaurant' AND o.user_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<com.example.fishing.vo.RestaurantOrderVO> selectMyRestaurantOrders(@Param("userId") Long userId);
}
