package com.example.fishing.controller;

import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.dto.CreateRestaurantOrderDTO;
import com.example.fishing.entity.RestaurantMenu;
import com.example.fishing.service.RestaurantMenuService;
import com.example.fishing.service.RestaurantOrderService;
import com.example.fishing.vo.RestaurantOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端餐厅接口
 */
@Tag(name = "用户端餐厅")
@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantMenuService restaurantMenuService;

    @Autowired
    private RestaurantOrderService restaurantOrderService;

    @GetMapping("/orders/my")
    @Operation(summary = "我的餐厅订单")
    public Result<List<RestaurantOrderVO>> myOrders() {
        Long userId = CurrentUser.get();
        return Result.success(restaurantOrderService.listMyOrders(userId));
    }

    @GetMapping("/{pondId}/menus")
    @Operation(summary = "餐厅菜单列表")
    public Result<List<RestaurantMenu>> listMenus(
            @Parameter(description = "鱼塘ID") @PathVariable Long pondId,
            @Parameter(description = "分类：fresh_fish-鲜鱼 cooked-加工菜品 drink-饮品") @RequestParam(required = false) String category) {
        return Result.success(restaurantMenuService.listByPondAndCategory(pondId, category));
    }

    @PostMapping("/{pondId}/orders")
    @Operation(summary = "创建餐厅订单")
    public Result<Long> createOrder(
            @Parameter(description = "鱼塘ID") @PathVariable Long pondId,
            @Validated @RequestBody CreateRestaurantOrderDTO dto) {
        Long userId = CurrentUser.get();
        Long orderId = restaurantOrderService.createOrder(userId, pondId, dto);
        return Result.success(orderId);
    }

    @PostMapping("/orders/{id}/pay")
    @Operation(summary = "模拟支付")
    public Result<Void> pay(
            @Parameter(description = "订单ID") @PathVariable Long id) {
        Long userId = CurrentUser.get();
        restaurantOrderService.pay(userId, id);
        return Result.success();
    }
}
