package com.example.fishing.service;

import com.example.fishing.entity.Pond;

import java.util.List;

/**
 * 鱼塘服务
 */
public interface PondService {

    List<Pond> listAll();

    /**
     * 按商家ID查询鱼塘列表
     */
    List<Pond> listByMerchantId(Long merchantId);

    Pond getById(Long id);

    void create(Pond pond);

    void update(Long id, Pond pond);

    void delete(Long id);

    /**
     * 校验指定鱼塘是否属于当前商家
     */
    void checkMerchantOwner(Long pondId, Long merchantId);
}
