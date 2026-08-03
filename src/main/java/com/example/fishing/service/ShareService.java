package com.example.fishing.service;

import com.example.fishing.vo.ShareBoardVO;

/**
 * 分享令牌服务
 */
public interface ShareService {

    /**
     * 创建分享令牌并返回分享 URL
     *
     * @param pondId  鱼塘ID
     * @param slotId  时段ID
     * @param baseUrl 前端基础地址
     * @return 完整分享 URL
     */
    String createShareUrl(Long pondId, Long slotId, String baseUrl);

    /**
     * 校验分享令牌有效性
     */
    void validateToken(Long pondId, Long slotId, String token);

    /**
     * 查询公开看板数据（已脱敏）
     */
    ShareBoardVO queryPublicBoard(Long pondId, Long slotId);
}
