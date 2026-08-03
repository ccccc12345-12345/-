package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.dto.ShopProductQuery;
import com.example.fishing.entity.ShopProduct;
import com.example.fishing.mapper.ShopProductMapper;
import com.example.fishing.service.ShopProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 商城商品服务实现
 */
@Service
public class ShopProductServiceImpl extends ServiceImpl<ShopProductMapper, ShopProduct> implements ShopProductService {

    @Override
    public IPage<ShopProduct> queryPage(ShopProductQuery query) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getMerchantId() != null, ShopProduct::getMerchantId, query.getMerchantId());
        wrapper.eq(query.getPondId() != null, ShopProduct::getPondId, query.getPondId());
        wrapper.eq(StringUtils.hasText(query.getCategory()), ShopProduct::getCategory, query.getCategory());
        wrapper.eq(StringUtils.hasText(query.getStatus()), ShopProduct::getStatus, query.getStatus());
        wrapper.like(StringUtils.hasText(query.getKeyword()), ShopProduct::getName, query.getKeyword());
        wrapper.orderByDesc(ShopProduct::getCreateTime);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public IPage<ShopProduct> queryUserPage(Long pondId, String category, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ShopProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopProduct::getStatus, "on");
        wrapper.gt(ShopProduct::getStock, 0);
        wrapper.eq(pondId != null, ShopProduct::getPondId, pondId);
        wrapper.eq(StringUtils.hasText(category), ShopProduct::getCategory, category);
        wrapper.like(StringUtils.hasText(keyword), ShopProduct::getName, keyword);
        wrapper.orderByDesc(ShopProduct::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void create(ShopProduct product, Long merchantId) {
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new BusinessException("商品价格不能为负数");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new BusinessException("商品库存不能为负数");
        }
        if (!StringUtils.hasText(product.getCategory())) {
            throw new BusinessException("商品分类不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        product.setMerchantId(merchantId);
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus("on");
        }
        product.setCreateTime(now);
        product.setUpdateTime(now);
        baseMapper.insert(product);
    }

    @Override
    public void update(Long id, ShopProduct product, Long merchantId) {
        ShopProduct exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("无权操作该商品");
        }
        if (!StringUtils.hasText(product.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new BusinessException("商品价格不能为负数");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new BusinessException("商品库存不能为负数");
        }
        exist.setName(product.getName().trim());
        exist.setCategory(product.getCategory());
        exist.setPrice(product.getPrice());
        exist.setStock(product.getStock());
        exist.setImageUrl(product.getImageUrl());
        exist.setDescription(product.getDescription());
        if (product.getPondId() != null) {
            exist.setPondId(product.getPondId());
        }
        exist.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(exist);
    }

    @Override
    public void updateStatus(Long id, String status, Long merchantId) {
        if (!"on".equals(status) && !"off".equals(status)) {
            throw new BusinessException("状态值不合法");
        }
        ShopProduct exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("无权操作该商品");
        }
        exist.setStatus(status);
        exist.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(exist);
    }

    @Override
    public void delete(Long id, Long merchantId) {
        ShopProduct exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在");
        }
        if (!merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("无权操作该商品");
        }
        baseMapper.deleteById(id);
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<ShopProduct> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShopProduct::getId, productId);
        wrapper.eq(ShopProduct::getStatus, "on");
        wrapper.ge(ShopProduct::getStock, quantity);
        wrapper.setSql("stock = stock - " + quantity);
        return baseMapper.update(null, wrapper) > 0;
    }
}
