package com.example.fishing.controller.merchant;

import com.example.fishing.common.Result;
import com.example.fishing.dto.MerchantStaffCreateDTO;
import com.example.fishing.dto.MerchantStaffResetPasswordDTO;
import com.example.fishing.dto.MerchantStaffStatusDTO;
import com.example.fishing.dto.MerchantStaffUpdateDTO;
import com.example.fishing.entity.MerchantStaff;
import com.example.fishing.service.MerchantStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 商家员工账号管理接口
 */
@Tag(name = "商家员工账号管理")
@RestController
@RequestMapping("/api/merchant/staff")
public class MerchantStaffController extends MerchantBaseController {

    @Autowired
    private MerchantStaffService merchantStaffService;

    @GetMapping("/list")
    @Operation(summary = "查询当前商家的员工列表")
    public Result<List<MerchantStaff>> list(
            @Parameter(description = "搜索关键词（姓名或手机号）") @RequestParam(required = false) String keyword) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        return Result.success(merchantStaffService.list(merchantId, keyword));
    }

    @PostMapping
    @Operation(summary = "新增员工")
    public Result<Void> create(@Valid @RequestBody MerchantStaffCreateDTO dto) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        merchantStaffService.create(merchantId, dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑员工")
    public Result<Void> update(
            @Parameter(description = "员工ID") @PathVariable Long id,
            @Valid @RequestBody MerchantStaffUpdateDTO dto) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        merchantStaffService.update(merchantId, id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新员工状态")
    public Result<Void> updateStatus(
            @Parameter(description = "员工ID") @PathVariable Long id,
            @Valid @RequestBody MerchantStaffStatusDTO dto) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        merchantStaffService.updateStatus(merchantId, id, dto.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置员工密码")
    public Result<String> resetPassword(
            @Parameter(description = "员工ID") @PathVariable Long id,
            @Valid @RequestBody MerchantStaffResetPasswordDTO dto) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        String newPassword = merchantStaffService.resetPassword(merchantId, id, dto.getNewPassword());
        return Result.success(newPassword);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除员工")
    public Result<Void> delete(@Parameter(description = "员工ID") @PathVariable Long id) {
        requireMerchantOwner();
        Long merchantId = requireMerchantId();
        merchantStaffService.delete(merchantId, id);
        return Result.success();
    }
}
