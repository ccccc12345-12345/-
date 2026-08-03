package com.example.fishing.controller.merchant;

import com.example.fishing.common.Result;
import com.example.fishing.dto.RestaurantMenuDTO;
import com.example.fishing.entity.RestaurantMenu;
import com.example.fishing.service.RestaurantMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家餐厅菜单管理接口
 */
@Tag(name = "商家餐厅菜单管理")
@RestController
@RequestMapping("/api/merchant/restaurant/menus")
public class MerchantRestaurantMenuController extends MerchantBaseController {

    @Autowired
    private RestaurantMenuService restaurantMenuService;

    @GetMapping
    @Operation(summary = "查询菜单列表")
    public Result<List<RestaurantMenu>> list(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId) {
        Long merchantId = requireMerchantId();
        return Result.success(restaurantMenuService.listByMerchant(merchantId, pondId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询菜单详情")
    public Result<RestaurantMenu> get(@Parameter(description = "菜单ID") @PathVariable Long id) {
        Long merchantId = requireMerchantId();
        restaurantMenuService.checkMerchantOwner(id, merchantId);
        return Result.success(restaurantMenuService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增菜单")
    public Result<Void> create(@Validated @RequestBody RestaurantMenuDTO dto) {
        Long merchantId = requireMerchantId();
        checkPondOwner(dto.getPondId());
        restaurantMenuService.create(merchantId, dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改菜单")
    public Result<Void> update(
            @Parameter(description = "菜单ID") @PathVariable Long id,
            @Validated @RequestBody RestaurantMenuDTO dto) {
        Long merchantId = requireMerchantId();
        checkPondOwner(dto.getPondId());
        restaurantMenuService.update(id, merchantId, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改菜单状态")
    public Result<Void> updateStatus(
            @Parameter(description = "菜单ID") @PathVariable Long id,
            @Parameter(description = "状态：on-上架 off-下架") @RequestParam String status) {
        Long merchantId = requireMerchantId();
        restaurantMenuService.updateStatus(id, merchantId, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    public Result<Void> delete(@Parameter(description = "菜单ID") @PathVariable Long id) {
        Long merchantId = requireMerchantId();
        restaurantMenuService.delete(id, merchantId);
        return Result.success();
    }

}
