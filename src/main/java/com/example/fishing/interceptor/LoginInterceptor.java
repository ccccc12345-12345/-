package com.example.fishing.interceptor;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.JwtUtil;
import com.example.fishing.entity.MerchantStaff;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.MerchantStaffMapper;
import com.example.fishing.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 登录拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private MerchantStaffMapper merchantStaffMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request);
        if (token == null || token.isEmpty()) {
            throw new BusinessException(401, "请先登录");
        }
        if (!JwtUtil.validate(token)) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        Long userId = JwtUtil.getUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }

        // 修复：若账号实际拥有鱼塘却被标记为平台管理员，自动识别为商家
        if (CurrentUser.ROLE_ADMIN == user.getRole() && jdbcTemplate != null) {
            Integer pondCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pond WHERE merchant_id = ?", Integer.class, user.getId());
            if (pondCount != null && pondCount > 0) {
                user.setRole(CurrentUser.ROLE_MERCHANT);
            }
        }

        Long staffId = null;
        Long staffMerchantId = null;
        String staffRole = null;
        if (CurrentUser.ROLE_STAFF == user.getRole() && user.getStaffId() != null) {
            MerchantStaff staff = merchantStaffMapper.selectById(user.getStaffId());
            if (staff != null) {
                staffId = staff.getId();
                staffMerchantId = staff.getMerchantId();
                staffRole = staff.getRole();
            }
        }

        CurrentUser.Context ctx = new CurrentUser.Context(
                userId, user.getRole(), user.getAdminType(), user.getPondId(),
                staffId, staffMerchantId, staffRole);
        CurrentUser.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.remove();
    }

    /**
     * 解析 Token：优先从 Authorization 头读取，否则从 URL 参数 token 读取（SSE 场景）。
     */
    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        String token = request.getParameter("token");
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }
        return null;
    }
}
