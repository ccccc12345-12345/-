package com.example.fishing.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽号记录导出 VO
 */
@Data
public class DrawResultExportVO {

    @ExcelProperty("抽号ID")
    private Long id;

    @ExcelProperty("预约ID")
    private Long reservationId;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("时段ID")
    private Long slotId;

    @ExcelProperty("钓位编号")
    private String spotCode;

    @ExcelProperty("抽号时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime drawTime;
}
