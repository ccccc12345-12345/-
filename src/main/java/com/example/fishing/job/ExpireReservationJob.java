package com.example.fishing.job;

import com.example.fishing.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约过期失效定时任务
 */
@Slf4j
@Component
public class ExpireReservationJob {

    @Autowired
    private ReservationService reservationService;

    /**
     * 每 5 分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void expire() {
        log.info("开始执行预约过期失效定时任务");
        reservationService.expirePendingReservations();
        log.info("预约过期失效定时任务执行完成");
    }
}
