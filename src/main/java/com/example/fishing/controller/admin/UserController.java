package com.example.fishing.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理接口（超级管理员权限）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserService sysUserService;

    @GetMapping
    @Operation(summary = "分页查询用户（仅超级管理员）")
    public Result<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "create_time") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可查看用户列表");
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like("phone", k).or().like("nickname", k).or().like("email", k));
        }
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, convertSortField(sortField));

        return Result.success(sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    private String convertSortField(String sortField) {
        if (sortField == null || sortField.isEmpty()) {
            return "create_time";
        }
        switch (sortField) {
            case "id":
                return "id";
            case "createTime":
                return "create_time";
            case "lastLoginTime":
                return "last_login_time";
            case "status":
                return "status";
            default:
                return "create_time";
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public Result<SysUser> detail(@Parameter(description = "用户ID") @PathVariable Long id) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可查看用户详情");
        }
        return Result.success(sysUserMapper.selectById(id));
    }

    @PutMapping("/{id}/admin-binding")
    @Operation(summary = "设置管理员类型及绑定鱼塘")
    public Result<Void> updateAdminBinding(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可配置权限");
        }
        Integer adminType = params.get("adminType") == null ? null : Integer.valueOf(params.get("adminType").toString());
        Long pondId = params.get("pondId") == null ? null : Long.valueOf(params.get("pondId").toString());
        sysUserService.updateAdminBinding(id, adminType, pondId);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> params) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        if (ctx == null || !ctx.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可配置权限");
        }
        Integer status = params.get("status");
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
        return Result.success();
    }
}
