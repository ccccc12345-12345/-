package com.example.fishing.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ReservationExportVO {

    @ExcelProperty("预约ID")
    private Long id;

    @ExcelProperty("用户手机号")
    private String userPhone;

    @ExcelProperty("用户昵称")
    private String userNickname;

    @ExcelProperty("时段日期")
    private String slotDate;

    @ExcelProperty("场次名称")
    private String slotName;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("钓位编号")
    private String spotCode;

    @ExcelProperty("创建时间")
    private String createTime;
}
