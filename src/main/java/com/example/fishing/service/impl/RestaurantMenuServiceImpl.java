package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fishing.common.BusinessException;
import com.example.fishing.dto.CookingMethodDTO;
import com.example.fishing.dto.RestaurantMenuDTO;
import com.example.fishing.entity.RestaurantMenu;
import com.example.fishing.mapper.RestaurantMenuMapper;
import com.example.fishing.service.PondService;
import com.example.fishing.service.RestaurantMenuService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 餐厅菜单服务实现
 */
@Service
public class RestaurantMenuServiceImpl implements RestaurantMenuService {

    @Autowired
    private RestaurantMenuMapper restaurantMenuMapper;

    @Autowired
    private PondService pondService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<RestaurantMenu> listByMerchant(Long merchantId, Long pondId) {
        LambdaQueryWrapper<RestaurantMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RestaurantMenu::getMerchantId, merchantId);
        wrapper.eq(RestaurantMenu::getDeleted, 0);
        if (pondId != null) {
            wrapper.eq(RestaurantMenu::getPondId, pondId);
        }
        wrapper.orderByAsc(RestaurantMenu::getCategory);
        wrapper.orderByDesc(RestaurantMenu::getCreateTime);
        return restaurantMenuMapper.selectList(wrapper);
    }

    @Override
    public List<RestaurantMenu> listByPondAndCategory(Long pondId, String category) {
        LambdaQueryWrapper<RestaurantMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RestaurantMenu::getPondId, pondId);
        wrapper.eq(RestaurantMenu::getStatus, "on");
        wrapper.eq(RestaurantMenu::getDeleted, 0);
        if (StringUtils.hasText(category)) {
            wrapper.eq(RestaurantMenu::getCategory, category);
        }
        wrapper.orderByDesc(RestaurantMenu::getIsSpecial);
        wrapper.orderByAsc(RestaurantMenu::getCategory);
        wrapper.orderByDesc(RestaurantMenu::getCreateTime);
        return restaurantMenuMapper.selectList(wrapper);
    }

    @Override
    public RestaurantMenu getById(Long id) {
        return restaurantMenuMapper.selectById(id);
    }

    @Override
    public void create(Long merchantId, RestaurantMenuDTO dto) {
        if (dto.getPondId() == null) {
            throw new BusinessException("鱼塘ID不能为空");
        }
        pondService.checkMerchantOwner(dto.getPondId(), merchantId);
        checkMenuDTO(dto);

        RestaurantMenu menu = new RestaurantMenu();
        menu.setPondId(dto.getPondId());
        menu.setMerchantId(merchantId);
        menu.setName(dto.getName().trim());
        menu.setCategory(dto.getCategory());
        menu.setPrice(dto.getPrice());
        menu.setStock(dto.getStock() == null ? -1 : dto.getStock());
        menu.setImageUrl(dto.getImageUrl());
        menu.setDescription(dto.getDescription());
        menu.setCookingMethods(toCookingMethodsJson(dto.getCookingMethods()));
        menu.setIsSpecial(dto.getIsSpecial() == null ? 0 : dto.getIsSpecial());
        menu.setStatus(dto.getStatus() == null ? "on" : dto.getStatus());
        menu.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        menu.setCreateTime(now);
        menu.setUpdateTime(now);
        restaurantMenuMapper.insert(menu);
    }

    @Override
    public void update(Long id, Long merchantId, RestaurantMenuDTO dto) {
        RestaurantMenu exist = getAndCheckOwner(id, merchantId);
        checkMenuDTO(dto);

        exist.setPondId(dto.getPondId());
        exist.setName(dto.getName().trim());
        exist.setCategory(dto.getCategory());
        exist.setPrice(dto.getPrice());
        exist.setStock(dto.getStock() == null ? -1 : dto.getStock());
        exist.setImageUrl(dto.getImageUrl());
        exist.setDescription(dto.getDescription());
        exist.setCookingMethods(toCookingMethodsJson(dto.getCookingMethods()));
        exist.setIsSpecial(dto.getIsSpecial() == null ? 0 : dto.getIsSpecial());
        exist.setStatus(dto.getStatus() == null ? exist.getStatus() : dto.getStatus());
        exist.setUpdateTime(LocalDateTime.now());
        restaurantMenuMapper.updateById(exist);
    }

    @Override
    public void updateStatus(Long id, Long merchantId, String status) {
        RestaurantMenu exist = getAndCheckOwner(id, merchantId);
        if (!"on".equals(status) && !"off".equals(status)) {
            throw new BusinessException("状态参数错误");
        }
        exist.setStatus(status);
        exist.setUpdateTime(LocalDateTime.now());
        restaurantMenuMapper.updateById(exist);
    }

    @Override
    public void delete(Long id, Long merchantId) {
        RestaurantMenu exist = getAndCheckOwner(id, merchantId);
        exist.setDeleted(1);
        exist.setUpdateTime(LocalDateTime.now());
        restaurantMenuMapper.updateById(exist);
    }

    @Override
    public void checkMerchantOwner(Long id, Long merchantId) {
        getAndCheckOwner(id, merchantId);
    }

    private RestaurantMenu getAndCheckOwner(Long id, Long merchantId) {
        RestaurantMenu menu = restaurantMenuMapper.selectById(id);
        if (menu == null || menu.getDeleted() == 1) {
            throw new BusinessException("菜单不存在");
        }
        if (!merchantId.equals(menu.getMerchantId())) {
            throw new BusinessException("无权操作该菜单");
        }
        return menu;
    }

    private void checkMenuDTO(RestaurantMenuDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("菜品名称不能为空");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new BusinessException("分类不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice() < 0) {
            throw new BusinessException("价格不能为负数");
        }
        if (dto.getStock() != null && dto.getStock() < -1) {
            throw new BusinessException("库存参数错误");
        }
        if (!CollectionUtils.isEmpty(dto.getCookingMethods())) {
            for (CookingMethodDTO method : dto.getCookingMethods()) {
                if (method.getName() == null || method.getName().trim().isEmpty()) {
                    throw new BusinessException("做法名称不能为空");
                }
                if (method.getPrice() == null || method.getPrice() < 0) {
                    throw new BusinessException("做法价格不能为负数");
                }
            }
        }
    }

    private String toCookingMethodsJson(List<CookingMethodDTO> methods) {
        if (CollectionUtils.isEmpty(methods)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(methods);
        } catch (JsonProcessingException e) {
            throw new BusinessException("做法数据格式错误");
        }
    }
}
