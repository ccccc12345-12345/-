package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.ShopOrderCreateDTO;
import com.example.fishing.dto.ShopOrderQuery;
import com.example.fishing.entity.ShopOrder;
import com.example.fishing.vo.ShopOrderVO;

/**
 * 商城订单服务
 */
public interface ShopOrderService extends IService<ShopOrder> {

    /**
     * 创建订单并扣减库存
     */
    ShopOrderVO createOrder(Long userId, ShopOrderCreateDTO dto);

    /**
     * 模拟支付
     */
    void pay(Long userId, Long orderId);

    /**
     * 查询我的订单
     */
    IPage<ShopOrderVO> myOrders(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 商家端分页查询订单
     */
    IPage<ShopOrderVO> queryPage(Long merchantId, ShopOrderQuery query);

    /**
     * 商家更新订单状态（完成/取消）
     */
    void updateStatus(Long merchantId, Long orderId, String status);
}
