package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.dto.MerchantStaffCreateDTO;
import com.example.fishing.dto.MerchantStaffUpdateDTO;
import com.example.fishing.entity.MerchantStaff;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.MerchantStaffMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.service.MerchantStaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商家员工服务实现
 */
@Service
public class MerchantStaffServiceImpl implements MerchantStaffService {

    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList("checker", "operator", "finance", "manager"));
    private static final Set<String> VALID_STATUS = new HashSet<>(Arrays.asList("normal", "disabled"));

    @Autowired
    private MerchantStaffMapper merchantStaffMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<MerchantStaff> list(Long merchantId, String keyword) {
        LambdaQueryWrapper<MerchantStaff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantStaff::getMerchantId, merchantId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MerchantStaff::getStaffName, keyword)
                    .or()
                    .like(MerchantStaff::getPhone, keyword));
        }
        wrapper.orderByDesc(MerchantStaff::getCreateTime);
        return merchantStaffMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long merchantId, MerchantStaffCreateDTO dto) {
        validateRole(dto.getRole());
        String phone = dto.getPhone().trim();
        checkPhoneAvailable(phone, null, null);

        LocalDateTime now = LocalDateTime.now();

        SysUser user = new SysUser();
        user.setPhone(phone);
        user.setNickname(dto.getStaffName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(CurrentUser.ROLE_STAFF);
        user.setStatus(1);
        user.setCreateTime(now);
        sysUserMapper.insert(user);

        MerchantStaff staff = new MerchantStaff();
        staff.setMerchantId(merchantId);
        staff.setUserId(user.getId());
        staff.setStaffName(dto.getStaffName().trim());
        staff.setPhone(phone);
        staff.setRole(dto.getRole());
        staff.setStatus("normal");
        staff.setCreateTime(now);
        staff.setUpdateTime(now);
        merchantStaffMapper.insert(staff);

        user.setStaffId(staff.getId());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long merchantId, Long id, MerchantStaffUpdateDTO dto) {
        validateRole(dto.getRole());
        MerchantStaff staff = getAndCheck(merchantId, id);
        String phone = dto.getPhone().trim();
        if (!phone.equals(staff.getPhone())) {
            checkPhoneAvailable(phone, id, staff.getUserId());
        }

        staff.setStaffName(dto.getStaffName().trim());
        staff.setPhone(phone);
        staff.setRole(dto.getRole());
        staff.setUpdateTime(LocalDateTime.now());
        merchantStaffMapper.updateById(staff);

        SysUser user = sysUserMapper.selectById(staff.getUserId());
        if (user != null) {
            user.setNickname(staff.getStaffName());
            user.setPhone(phone);
            sysUserMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long merchantId, Long id, String status) {
        if (!VALID_STATUS.contains(status)) {
            throw new BusinessException("状态不合法");
        }
        MerchantStaff staff = getAndCheck(merchantId, id);
        staff.setStatus(status);
        staff.setUpdateTime(LocalDateTime.now());
        merchantStaffMapper.updateById(staff);

        SysUser user = sysUserMapper.selectById(staff.getUserId());
        if (user != null) {
            user.setStatus("normal".equals(status) ? 1 : 0);
            sysUserMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long merchantId, Long id, String newPassword) {
        MerchantStaff staff = getAndCheck(merchantId, id);
        SysUser user = sysUserMapper.selectById(staff.getUserId());
        if (user == null) {
            throw new BusinessException("关联系统账号不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return newPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long merchantId, Long id) {
        MerchantStaff staff = getAndCheck(merchantId, id);
        staff.setStatus("disabled");
        staff.setUpdateTime(LocalDateTime.now());
        merchantStaffMapper.updateById(staff);

        SysUser user = sysUserMapper.selectById(staff.getUserId());
        if (user != null) {
            user.setStatus(0);
            sysUserMapper.updateById(user);
        }
    }

    private MerchantStaff getAndCheck(Long merchantId, Long id) {
        MerchantStaff staff = merchantStaffMapper.selectById(id);
        if (staff == null) {
            throw new BusinessException("员工不存在");
        }
        if (!merchantId.equals(staff.getMerchantId())) {
            throw new BusinessException("无权操作该员工");
        }
        return staff;
    }

    private void validateRole(String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new BusinessException("员工角色不合法");
        }
    }

    private void checkPhoneAvailable(String phone, Long excludeStaffId, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getPhone, phone);
        if (excludeUserId != null) {
            userWrapper.ne(SysUser::getId, excludeUserId);
        }
        SysUser existUser = sysUserMapper.selectOne(userWrapper);
        if (existUser != null) {
            throw new BusinessException("手机号已被注册");
        }

        LambdaQueryWrapper<MerchantStaff> staffWrapper = new LambdaQueryWrapper<>();
        staffWrapper.eq(MerchantStaff::getPhone, phone);
        if (excludeStaffId != null) {
            staffWrapper.ne(MerchantStaff::getId, excludeStaffId);
        }
        if (merchantStaffMapper.selectCount(staffWrapper) > 0) {
            throw new BusinessException("手机号已被其他员工使用");
        }
    }
}
