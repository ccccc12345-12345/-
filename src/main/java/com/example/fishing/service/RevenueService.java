package com.example.fishing.service;

import com.example.fishing.dto.RevenueQuery;
import com.example.fishing.vo.RevenueVO;

import java.math.BigDecimal;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 收益统计服务
 */
public interface RevenueService {

    Map<String, BigDecimal> summary(Long pondId);

    List<RevenueVO> list(RevenueQuery query);

    void exportExcel(RevenueQuery query, HttpServletResponse response) throws IOException;
}
