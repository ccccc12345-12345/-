package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.entity.InviteCode;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.InviteCodeMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private InviteCodeMapper inviteCodeMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Long register(String phone, String nickname, String password) {
        // 校验手机号是否已存在
        SysUser exist = findByPhone(phone);
        if (exist != null) {
            throw new RuntimeException("手机号已注册");
        }

        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(0); // 普通用户
        user.setStatus(1); // 启用
        user.setCreateTime(LocalDateTime.now());
        baseMapper.insert(user);
        return user.getId();
    }

    @Override
    public SysUser findByPhone(String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getPhone, phone);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updateAdminBinding(Long userId, Integer adminType, Long pondId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (adminType != null && adminType != 0 && adminType != 1) {
            throw new RuntimeException("管理员类型不合法");
        }
        user.setAdminType(adminType);
        user.setPondId(pondId);
        baseMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerMerchant(String phone, String nickname, String password, String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new RuntimeException("邀请码不能为空");
        }
        LambdaQueryWrapper<InviteCode> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(InviteCode::getCode, inviteCode.trim()).eq(InviteCode::getUsed, 0);
        InviteCode code = inviteCodeMapper.selectOne(codeWrapper);
        if (code == null) {
            throw new RuntimeException("邀请码无效或已被使用");
        }

        SysUser exist = findByPhone(phone);
        if (exist != null) {
            throw new RuntimeException("手机号已注册");
        }

        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(CurrentUser.ROLE_MERCHANT);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        baseMapper.insert(user);

        // 标记邀请码已使用
        code.setUsed(1);
        code.setMerchantId(user.getId());
        inviteCodeMapper.updateById(code);

        return user.getId();
    }
}
