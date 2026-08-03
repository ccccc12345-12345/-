package com.example.fishing.controller.merchant;

import com.example.fishing.common.Result;
import com.example.fishing.service.RestaurantOrderService;
import com.example.fishing.vo.RestaurantOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家餐厅订单管理接口
 */
@Tag(name = "商家餐厅订单管理")
@RestController
@RequestMapping("/api/merchant/restaurant/orders")
public class MerchantRestaurantOrderController extends MerchantBaseController {

    @Autowired
    private RestaurantOrderService restaurantOrderService;

    @GetMapping
    @Operation(summary = "餐厅订单列表")
    public Result<List<RestaurantOrderVO>> list(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId,
            @Parameter(description = "状态：pending-待处理 cooking-制作中 delivered-已配送 completed-已完成") @RequestParam(required = false) String status) {
        Long merchantId = requireMerchantId();
        return Result.success(restaurantOrderService.listMerchantOrders(merchantId, pondId, status));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态")
    public Result<Void> updateStatus(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "状态：pending-待处理 cooking-制作中 delivered-已配送 completed-已完成") @RequestParam String status) {
        Long merchantId = requireMerchantId();
        restaurantOrderService.updateStatus(id, merchantId, status);
        return Result.success();
    }
}
