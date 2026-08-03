package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.CatchRecordDTO;
import com.example.fishing.entity.CatchRecord;
import com.example.fishing.vo.CatchRecordVO;

import java.util.List;

/**
 * 渔获记录服务
 */
public interface CatchRecordService extends IService<CatchRecord> {

    /**
     * 用户创建渔获记录
     */
    Long create(Long userId, CatchRecordDTO dto);

    /**
     * 查询当前用户的渔获记录
     */
    List<CatchRecordVO> listByUser(Long userId);

    /**
     * 用户申请回收
     */
    void applyRecycle(Long userId, Long recordId);

    /**
     * 商家分页查询渔获记录
     */
    IPage<CatchRecordVO> queryPage(Long merchantId, Long pondId, String status, Integer pageNum, Integer pageSize);

    /**
     * 商家确认回收
     */
    void confirmRecycle(Long merchantId, Long recordId, Integer recyclePrice);
}
