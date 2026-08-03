package com.example.fishing.service;

import com.example.fishing.dto.RestaurantMenuDTO;
import com.example.fishing.entity.RestaurantMenu;

import java.util.List;

/**
 * 餐厅菜单服务
 */
public interface RestaurantMenuService {

    /**
     * 按商家和鱼塘查询菜单列表
     */
    List<RestaurantMenu> listByMerchant(Long merchantId, Long pondId);

    /**
     * 按鱼塘和分类查询上架菜单（用户端）
     */
    List<RestaurantMenu> listByPondAndCategory(Long pondId, String category);

    /**
     * 查询菜单详情
     */
    RestaurantMenu getById(Long id);

    /**
     * 创建菜单
     */
    void create(Long merchantId, RestaurantMenuDTO dto);

    /**
     * 修改菜单
     */
    void update(Long id, Long merchantId, RestaurantMenuDTO dto);

    /**
     * 修改菜单状态
     */
    void updateStatus(Long id, Long merchantId, String status);

    /**
     * 删除菜单
     */
    void delete(Long id, Long merchantId);

    /**
     * 校验菜单属于商家
     */
    void checkMerchantOwner(Long id, Long merchantId);
}
