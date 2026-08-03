package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.Constants;
import com.example.fishing.entity.DrawResult;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.Reservation;
import com.example.fishing.entity.ShareToken;
import com.example.fishing.entity.SysUser;
import com.example.fishing.entity.TimeSlot;
import com.example.fishing.mapper.DrawResultMapper;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.ReservationMapper;
import com.example.fishing.mapper.ShareTokenMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.mapper.TimeSlotMapper;
import com.example.fishing.service.ShareService;
import com.example.fishing.vo.ShareBoardVO;
import com.example.fishing.vo.SpotBoardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 分享令牌服务实现
 */
@Service
public class ShareServiceImpl implements ShareService {

    @Autowired
    private ShareTokenMapper shareTokenMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private DrawResultMapper drawResultMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createShareUrl(Long pondId, Long slotId, String baseUrl) {
        if (pondId == null || slotId == null) {
            throw new BusinessException("鱼塘和时段不能为空");
        }

        // 生成唯一 token
        String token = UUID.randomUUID().toString().replace("-", "");
        ShareToken shareToken = new ShareToken();
        shareToken.setToken(token);
        shareToken.setPondId(pondId);
        shareToken.setSlotId(slotId);
        shareToken.setExpireTime(LocalDateTime.now().plusHours(2));
        shareToken.setCreateTime(LocalDateTime.now());
        shareTokenMapper.insert(shareToken);

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3001";
        }
        return baseUrl + "/share?pondId=" + pondId + "&slotId=" + slotId + "&token=" + token;
    }

    @Override
    public void validateToken(Long pondId, Long slotId, String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException("分享令牌不能为空");
        }
        LambdaQueryWrapper<ShareToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareToken::getToken, token.trim())
                .eq(ShareToken::getPondId, pondId)
                .eq(ShareToken::getSlotId, slotId)
                .gt(ShareToken::getExpireTime, LocalDateTime.now());
        ShareToken shareToken = shareTokenMapper.selectOne(wrapper);
        if (shareToken == null) {
            throw new BusinessException("分享链接已过期或无效");
        }
    }

    @Override
    public ShareBoardVO queryPublicBoard(Long pondId, Long slotId) {
        ShareBoardVO boardVo = new ShareBoardVO();
        boardVo.setPondId(pondId);
        boardVo.setSlotId(slotId);

        Pond pond = pondMapper.selectById(pondId);
        if (pond != null) {
            boardVo.setPondName(pond.getName());
        }
        TimeSlot slot = timeSlotMapper.selectById(slotId);
        if (slot != null) {
            boardVo.setSlotDate(String.valueOf(slot.getSlotDate()));
            boardVo.setSlotName(slot.getSlotName());
        }

        // 查询钓位
        List<FishingSpot> spots = fishingSpotMapper.selectList(
                new QueryWrapper<FishingSpot>().eq("pond_id", pondId).orderByAsc("spot_code"));
        if (spots.isEmpty()) {
            boardVo.setSpots(new ArrayList<>());
            return boardVo;
        }

        // 查询时段下有效预约
        List<String> activeStatus = new ArrayList<>();
        activeStatus.add(Constants.RESERVATION_PENDING);
        activeStatus.add(Constants.RESERVATION_DRAWN);
        activeStatus.add(Constants.RESERVATION_CHECKED_IN);
        List<Reservation> reservations = reservationMapper.selectList(
                new QueryWrapper<Reservation>()
                        .eq("slot_id", slotId)
                        .in("status", activeStatus));

        // 查询抽号结果
        List<DrawResult> drawResults = drawResultMapper.selectList(
                new QueryWrapper<DrawResult>().eq("slot_id", slotId));
        Map<Long, DrawResult> reservationDrawMap = drawResults.stream()
                .collect(Collectors.toMap(DrawResult::getReservationId, d -> d, (a, b) -> a));

        // 按钓位查找关联预约
        Map<Long, Reservation> spotReservationMap = new HashMap<>();
        for (Reservation r : reservations) {
            DrawResult dr = reservationDrawMap.get(r.getId());
            if (dr != null && dr.getSpotId() != null) {
                spotReservationMap.put(dr.getSpotId(), r);
            }
        }

        // 用户信息（公开页面仅展示昵称和后四位手机号）
        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? new HashMap<>() : sysUserMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<SpotBoardVO> result = new ArrayList<>();
        for (FishingSpot spot : spots) {
            SpotBoardVO vo = new SpotBoardVO();
            vo.setSpotId(spot.getId());
            vo.setSpotCode(spot.getSpotCode());

            if (Constants.SPOT_DISABLED.equals(spot.getStatus())) {
                vo.setStatus("维修中");
            } else {
                Reservation r = spotReservationMap.get(spot.getId());
                if (r != null) {
                    vo.setReservationId(r.getId());
                    vo.setReservationStatus(r.getStatus());
                    if (Constants.RESERVATION_CHECKED_IN.equals(r.getStatus())) {
                        vo.setStatus("已核销");
                    } else {
                        vo.setStatus("已预约");
                    }
                    SysUser user = userMap.get(r.getUserId());
                    if (user != null) {
                        vo.setUserNickname(user.getNickname());
                        vo.setUserPhone(maskPhone(user.getPhone()));
                    }
                } else {
                    vo.setStatus("空闲");
                }
            }
            result.add(vo);
        }
        boardVo.setSpots(result);
        return boardVo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(phone.length() - 4);
    }
}
