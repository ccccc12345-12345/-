package com.example.fishing.controller;

import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.JwtUtil;
import com.example.fishing.common.Result;
import com.example.fishing.dto.LoginDTO;
import com.example.fishing.dto.RegisterDTO;
import com.example.fishing.entity.MerchantStaff;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.MerchantStaffMapper;
import com.example.fishing.service.CaptchaService;
import com.example.fishing.service.SysUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "登录注册")
@RestController
@RequestMapping("/api")
public class LoginController {

    @Resource
    private SysUserService sysUserService;

    @Autowired
    private MerchantStaffMapper merchantStaffMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOGIN_FAIL_LOCK_MINUTES = 15;

    @PostMapping("/register")
    @Operation(summary = "用户/商家注册")
    public Result<Long> register(@Validated @RequestBody RegisterDTO dto) {
        if (!captchaService.validate(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            return Result.error(400, "验证码错误或已过期");
        }
        if (!dto.passwordMatch()) {
            return Result.error(400, "两次密码不一致");
        }
        if ("merchant".equals(dto.getRole()) && (dto.getInviteCode() == null || dto.getInviteCode().trim().isEmpty())) {
            return Result.error(400, "商家注册需要填写邀请码");
        }

        try {
            Long userId;
            if ("merchant".equals(dto.getRole())) {
                userId = sysUserService.registerMerchant(dto.getPhone(), dto.getNickname(), dto.getPassword(), dto.getInviteCode());
            } else {
                userId = sysUserService.register(dto.getPhone(), dto.getNickname(), dto.getPassword());
            }
            log.info("用户注册成功: phone={}, role={}", dto.getPhone(), dto.getRole());
            return Result.success(userId);
        } catch (RuntimeException e) {
            log.warn("用户注册失败: phone={}, reason={}", dto.getPhone(), e.getMessage());
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录，获取 JWT token")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO dto) {
        String phone = dto.getUsername();

        String failKey = LOGIN_FAIL_PREFIX + phone;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= MAX_LOGIN_FAIL) {
            log.warn("登录次数过多，账号已临时锁定: phone={}", phone);
            return Result.error(429, "登录失败次数过多，请 15 分钟后重试");
        }

        if (!isDemoAccount(phone) && !captchaService.validate(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        SysUser user = sysUserService.findByPhone(phone);
        if (user == null) {
            incrementLoginFail(failKey);
            return Result.error(400, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(400, "账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            incrementLoginFail(failKey);
            return Result.error(400, "用户名或密码错误");
        }

        redisTemplate.delete(failKey);

        // 修复：若账号实际拥有鱼塘却被标记为平台管理员，自动识别为商家并持久化
        if (Integer.valueOf(CurrentUser.ROLE_ADMIN).equals(user.getRole())) {
            Integer pondCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pond WHERE merchant_id = ?", Integer.class, user.getId());
            if (pondCount != null && pondCount > 0) {
                user.setRole(CurrentUser.ROLE_MERCHANT);
            }
        }

        user.setLastLoginTime(LocalDateTime.now());
        sysUserService.updateById(user);

        String token = JwtUtil.generateToken(user.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("userId", String.valueOf(user.getId()));
        map.put("role", user.getRole());
        map.put("adminType", user.getAdminType());
        map.put("pondId", user.getPondId());

        if (user.getRole() != null && user.getRole() == 3 && user.getStaffId() != null) {
            MerchantStaff staff = merchantStaffMapper.selectById(user.getStaffId());
            if (staff != null) {
                map.put("staffId", String.valueOf(staff.getId()));
                map.put("merchantId", String.valueOf(staff.getMerchantId()));
                map.put("staffRole", staff.getRole());
                map.put("staffName", staff.getStaffName());
            }
        }

        log.info("用户登录成功: phone={}, role={}", phone, user.getRole());
        return Result.success(map);
    }

    private void incrementLoginFail(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            redisTemplate.expire(failKey, LOGIN_FAIL_LOCK_MINUTES, TimeUnit.MINUTES);
        }
    }

    private boolean isDemoAccount(String phone) {
        return "18800000001".equals(phone) || "18800000002".equals(phone) || "merchant".equals(phone);
    }
}
