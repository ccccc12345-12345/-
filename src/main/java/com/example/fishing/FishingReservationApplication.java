package com.example.fishing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 钓鱼场预约系统启动类
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.example.fishing.mapper")
public class FishingReservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(FishingReservationApplication.class, args);
    }
}
