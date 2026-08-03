package com.example.fishing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import com.example.fishing.dto.ShopOrderCreateDTO;
import com.example.fishing.entity.ShopProduct;
import com.example.fishing.service.ShopOrderService;
import com.example.fishing.service.ShopProductService;
import com.example.fishing.vo.ShopOrderVO;
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

/**
 * 用户端商城接口
 */
@Tag(name = "用户端商城")
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ShopProductService shopProductService;

    @Autowired
    private ShopOrderService shopOrderService;

    @GetMapping("/products")
    @Operation(summary = "商品列表")
    public Result<IPage<ShopProduct>> listProducts(
            @Parameter(description = "鱼塘ID") @RequestParam(required = false) Long pondId,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<ShopProduct> page = shopProductService.queryUserPage(pondId, category, keyword, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "商品详情")
    public Result<ShopProduct> getProduct(@Parameter(description = "商品ID") @PathVariable Long id) {
        ShopProduct product = shopProductService.getById(id);
        if (product == null || !"on".equals(product.getStatus())) {
            return Result.error("商品不存在或已下架");
        }
        return Result.success(product);
    }

    @PostMapping("/orders")
    @Operation(summary = "创建订单")
    public Result<ShopOrderVO> createOrder(@Validated @RequestBody ShopOrderCreateDTO dto) {
        ShopOrderVO vo = shopOrderService.createOrder(CurrentUser.get(), dto);
        return Result.success(vo);
    }

    @PostMapping("/orders/{id}/pay")
    @Operation(summary = "模拟支付")
    public Result<Void> pay(@Parameter(description = "订单ID") @PathVariable Long id) {
        shopOrderService.pay(CurrentUser.get(), id);
        return Result.success();
    }

    @GetMapping("/orders/my")
    @Operation(summary = "我的商城订单")
    public Result<IPage<ShopOrderVO>> myOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<ShopOrderVO> page = shopOrderService.myOrders(CurrentUser.get(), pageNum, pageSize);
        return Result.success(page);
    }
}
