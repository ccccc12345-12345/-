package com.example.fishing.service;

import com.example.fishing.dto.CreateRestaurantOrderDTO;
import com.example.fishing.vo.RestaurantOrderVO;

import java.util.List;

/**
 * 餐厅订单服务
 */
public interface RestaurantOrderService {

    /**
     * 创建餐厅订单
     */
    Long createOrder(Long userId, Long pondId, CreateRestaurantOrderDTO dto);

    /**
     * 查询商家餐厅订单列表
     */
    List<RestaurantOrderVO> listMerchantOrders(Long merchantId, Long pondId, String status);

    /**
     * 查询当前用户的餐厅订单列表
     */
    List<RestaurantOrderVO> listMyOrders(Long userId);

    /**
     * 模拟支付
     */
    void pay(Long userId, Long orderId);

    /**
     * 商家更新订单状态
     */
    void updateStatus(Long orderId, Long merchantId, String status);
}
