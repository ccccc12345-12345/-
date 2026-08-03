package com.example.fishing.service;

import com.example.fishing.vo.CheckinResultVO;

/**
 * 核销服务
 */
public interface CheckinService {

    CheckinResultVO checkin(String checkinCode);

    CheckinResultVO queryByCode(String checkinCode);
}
