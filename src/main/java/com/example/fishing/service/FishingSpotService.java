package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.FishingSpotDTO;
import com.example.fishing.entity.FishingSpot;

/**
 * 钓位服务
 */
public interface FishingSpotService extends IService<FishingSpot> {

    void create(FishingSpotDTO dto);

    void update(Long id, FishingSpotDTO dto);

    IPage<FishingSpot> pageList(Integer pageNum, Integer pageSize, Long pondId);
}
