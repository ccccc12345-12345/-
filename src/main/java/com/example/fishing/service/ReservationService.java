package com.example.fishing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fishing.dto.ReservationDTO;
import com.example.fishing.dto.ReservationQuery;
import com.example.fishing.entity.Reservation;
import com.example.fishing.vo.ReservationVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 预约服务
 */
public interface ReservationService extends IService<Reservation> {

    /**
     * 用户预约
     */
    Long book(Long userId, ReservationDTO dto);

    /**
     * 用户预约并立即分配钓位。
     */
    ReservationVO bookWithSpot(Long userId, ReservationDTO dto);

    /**
     * 取消预约
     */
    void cancel(Long userId, Long reservationId);

    /**
     * 管理员手动取消
     */
    void adminCancel(Long reservationId);

    /**
     * 管理员手动取消（带原因）
     */
    void adminCancel(Long reservationId, String reason);

    IPage<Reservation> queryPage(ReservationQuery query);

    /**
     * 分页查询预约记录（含用户与时段信息）
     */
    IPage<ReservationVO> queryPageVo(ReservationQuery query);

    /**
     * 查询当前用户的预约记录（含时段与钓位信息）
     */
    List<ReservationVO> myReservations(Long userId);

    /**
     * 将抽号已结束时段的待抽号预约置为过期失效
     */
    void expirePendingReservations();

    /**
     * 导出预约记录 Excel
     */
    void exportExcel(ReservationQuery query, HttpServletResponse response) throws IOException;

    /**
     * 修改预约实际收费金额
     */
    void updateActualFee(Long reservationId, java.math.BigDecimal actualFee);
}
