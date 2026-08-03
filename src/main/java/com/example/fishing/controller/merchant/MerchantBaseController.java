package com.example.fishing.controller.merchant;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.service.PondService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 商家端控制器抽象基类
 */
public abstract class MerchantBaseController {

    @Autowired
    protected PondService pondService;

    /**
     * 获取当前商家ID。商家账号直接返回其用户ID；员工账号返回关联商家ID。
     */
    protected Long requireMerchantId() {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null) {
            throw new BusinessException("请先登录");
        }
        Long merchantId = ctx.getMerchantId();
        if (merchantId == null) {
            throw new BusinessException("无权访问商家后台");
        }
        return merchantId;
    }

    /**
     * 校验当前用户是否为商家老板（role=merchant），员工不允许访问
     */
    protected void requireMerchantOwner() {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isMerchant()) {
            throw new BusinessException("仅商家老板可访问");
        }
    }

    /**
     * 校验当前用户是否为商家老板或店长（manager）
     */
    protected void requireMerchantOrManager() {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null) {
            throw new BusinessException("请先登录");
        }
        if (ctx.isMerchant()) {
            return;
        }
        if (ctx.isStaff() && "manager".equals(ctx.getStaffRole())) {
            return;
        }
        throw new BusinessException("无权访问该功能");
    }

    /**
     * 校验当前用户是否为商家老板或指定角色的员工
     */
    protected void requireStaffRoles(String... allowedRoles) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null) {
            throw new BusinessException("请先登录");
        }
        if (ctx.isMerchant()) {
            return;
        }
        if (ctx.isStaff()) {
            Set<String> set = new HashSet<>(Arrays.asList(allowedRoles));
            if (set.contains(ctx.getStaffRole())) {
                return;
            }
        }
        throw new BusinessException("无权访问该功能");
    }

    /**
     * 校验指定鱼塘是否属于当前商家
     */
    protected void checkPondOwner(Long pondId) {
        pondService.checkMerchantOwner(pondId, requireMerchantId());
    }
}
