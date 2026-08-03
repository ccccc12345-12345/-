package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.ShopProductQuery;
import com.example.fishing.entity.ShopProduct;

/**
 * 商城商品服务
 */
public interface ShopProductService extends IService<ShopProduct> {

    /**
     * 商家端分页查询商品
     */
    IPage<ShopProduct> queryPage(ShopProductQuery query);

    /**
     * 用户端分页查询商品
     */
    IPage<ShopProduct> queryUserPage(Long pondId, String category, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 创建商品
     */
    void create(ShopProduct product, Long merchantId);

    /**
     * 修改商品
     */
    void update(Long id, ShopProduct product, Long merchantId);

    /**
     * 修改商品状态
     */
    void updateStatus(Long id, String status, Long merchantId);

    /**
     * 删除商品
     */
    void delete(Long id, Long merchantId);

    /**
     * 扣减库存
     */
    boolean deductStock(Long productId, Integer quantity);
}
