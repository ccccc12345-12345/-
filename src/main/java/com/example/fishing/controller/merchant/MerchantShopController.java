package com.example.fishing.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.fishing.common.Result;
import com.example.fishing.dto.ShopOrderQuery;
import com.example.fishing.dto.ShopProductQuery;
import com.example.fishing.entity.ShopProduct;
import com.example.fishing.service.ShopOrderService;
import com.example.fishing.service.ShopProductService;
import com.example.fishing.vo.ShopOrderVO;
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

/**
 * 商家端商城商品管理接口
 */
@Tag(name = "商家端商城商品管理")
@RestController
@RequestMapping("/api/merchant/shop")
public class MerchantShopController extends MerchantBaseController {

    @Autowired
    private ShopProductService shopProductService;

    @Autowired
    private ShopOrderService shopOrderService;

    @GetMapping("/products")
    @Operation(summary = "商品列表")
    public Result<IPage<ShopProduct>> listProducts(ShopProductQuery query) {
        query.setMerchantId(requireMerchantId());
        IPage<ShopProduct> page = shopProductService.queryPage(query);
        return Result.success(page);
    }

    @PostMapping("/products")
    @Operation(summary = "新增商品")
    public Result<Void> create(@Validated @RequestBody ShopProduct product) {
        Long merchantId = requireMerchantId();
        shopProductService.create(product, merchantId);
        return Result.success();
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "编辑商品")
    public Result<Void> update(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Validated @RequestBody ShopProduct product) {
        Long merchantId = requireMerchantId();
        shopProductService.update(id, product, merchantId);
        return Result.success();
    }

    @PutMapping("/products/{id}/status")
    @Operation(summary = "上下架")
    public Result<Void> updateStatus(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "状态：on-上架 off-下架") @RequestParam String status) {
        Long merchantId = requireMerchantId();
        shopProductService.updateStatus(id, status, merchantId);
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> delete(@Parameter(description = "商品ID") @PathVariable Long id) {
        Long merchantId = requireMerchantId();
        shopProductService.delete(id, merchantId);
        return Result.success();
    }

    @GetMapping("/orders")
    @Operation(summary = "商城订单列表")
    public Result<IPage<ShopOrderVO>> listOrders(ShopOrderQuery query) {
        Long merchantId = requireMerchantId();
        return Result.success(shopOrderService.queryPage(merchantId, query));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "更新商城订单状态")
    public Result<Void> updateOrderStatus(
            @Parameter(description = "订单ID") @PathVariable Long id,
            @Parameter(description = "状态：completed-完成 cancelled-取消") @RequestParam String status) {
        Long merchantId = requireMerchantId();
        shopOrderService.updateStatus(merchantId, id, status);
        return Result.success();
    }
}
