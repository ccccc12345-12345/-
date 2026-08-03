package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.DrawQuery;
import com.example.fishing.entity.DrawResult;
import com.example.fishing.vo.DrawResultVO;
import com.example.fishing.vo.MissedDrawVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 抽号服务
 */
public interface DrawResultService extends IService<DrawResult> {

    /**
     * 一键抽号
     */
    String draw(Long userId, Long reservationId);

    IPage<DrawResult> queryPage(DrawQuery query);

    /**
     * 分页查询抽号记录（含用户与时段信息）
     */
    IPage<DrawResultVO> queryPageVo(DrawQuery query);

    /**
     * 查询指定场次未参与抽号的用户名单
     */
    List<MissedDrawVO> missedList(Long slotId);

    /**
     * 查询当前用户的抽号记录（含时段与钓位信息）
     */
    List<DrawResultVO> myDraws(Long userId);

    void exportExcel(DrawQuery query, HttpServletResponse response) throws IOException;
}
