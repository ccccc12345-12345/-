package com.example.fishing.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.fishing.common.BusinessException;
import com.example.fishing.dto.CookingMethodDTO;
import com.example.fishing.dto.CreateRestaurantOrderDTO;
import com.example.fishing.dto.RestaurantOrderItemDTO;
import com.example.fishing.entity.FishingSpot;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.RestaurantMenu;
import com.example.fishing.entity.ShopOrder;
import com.example.fishing.entity.ShopOrderItem;
import com.example.fishing.mapper.FishingSpotMapper;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.RestaurantMenuMapper;
import com.example.fishing.mapper.ShopOrderItemMapper;
import com.example.fishing.mapper.ShopOrderMapper;
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.RestaurantOrderService;
import com.example.fishing.vo.RestaurantOrderItemVO;
import com.example.fishing.vo.RestaurantOrderVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 餐厅订单服务实现
 */
@Service
public class RestaurantOrderServiceImpl implements RestaurantOrderService {

    @Autowired
    private ShopOrderMapper shopOrderMapper;

    @Autowired
    private ShopOrderItemMapper shopOrderItemMapper;

    @Autowired
    private RestaurantMenuMapper restaurantMenuMapper;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private FishingSpotMapper fishingSpotMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ORDER_PREFIX = "R";

    private static final Map<String, String> STATUS_FLOW = new LinkedHashMap<>();

    static {
        STATUS_FLOW.put("pending", "cooking");
        STATUS_FLOW.put("cooking", "delivered");
        STATUS_FLOW.put("delivered", "completed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long pondId, CreateRestaurantOrderDTO dto) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        if (pondId == null) {
            throw new BusinessException("鱼塘ID不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("订单项不能为空");
        }

        Pond pond = pondMapper.selectById(pondId);
        if (pond == null) {
            throw new BusinessException("鱼塘不存在");
        }

        if (dto.getSpotId() != null) {
            FishingSpot spot = fishingSpotMapper.selectById(dto.getSpotId());
            if (spot == null || !spot.getPondId().equals(pondId)) {
                throw new BusinessException("钓位不存在或不在当前鱼塘");
            }
        }

        int totalAmount = 0;
        List<ShopOrderItem> orderItems = new ArrayList<>();
        for (RestaurantOrderItemDTO item : dto.getItems()) {
            RestaurantMenu menu = restaurantMenuMapper.selectById(item.getMenuId());
            if (menu == null || menu.getDeleted() == 1) {
                throw new BusinessException("菜品不存在");
            }
            if (!menu.getPondId().equals(pondId)) {
                throw new BusinessException("菜品不在当前鱼塘");
            }
            if (!"on".equals(menu.getStatus())) {
                throw new BusinessException("菜品【" + menu.getName() + "】已下架");
            }
            if (menu.getStock() != null && menu.getStock() >= 0 && menu.getStock() < item.getQuantity()) {
                throw new BusinessException("菜品【" + menu.getName() + "】库存不足");
            }

            List<CookingMethodDTO> methods = parseCookingMethods(menu.getCookingMethods());
            int unitPrice = menu.getPrice();
            String cookingMethod = null;
            if (StringUtils.hasText(item.getCookingMethod())) {
                CookingMethodDTO selected = methods.stream()
                        .filter(m -> item.getCookingMethod().equals(m.getName()))
                        .findFirst()
                        .orElse(null);
                if (selected == null) {
                    throw new BusinessException("菜品【" + menu.getName() + "】不存在做法【" + item.getCookingMethod() + "】");
                }
                unitPrice += selected.getPrice();
                cookingMethod = selected.getName();
            }

            ShopOrderItem orderItem = new ShopOrderItem();
            orderItem.setMenuId(menu.getId());
            orderItem.setMenuName(menu.getName());
            orderItem.setPrice(unitPrice);
            orderItem.setCookingMethod(cookingMethod);
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);

            totalAmount += unitPrice * item.getQuantity();
        }

        ShopOrder order = new ShopOrder();
        order.setOrderNo(generateOrderNo());
        order.setOrderType("restaurant");
        order.setUserId(userId);
        order.setPondId(pondId);
        order.setMerchantId(pond.getMerchantId());
        order.setSpotId(dto.getSpotId());
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");
        order.setRemark(dto.getRemark());
        order.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        order.setCreateTime(now);
        order.setUpdateTime(now);
        shopOrderMapper.insert(order);

        for (ShopOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItem.setCreateTime(now);
            shopOrderItemMapper.insert(orderItem);

            RestaurantMenu menu = restaurantMenuMapper.selectById(orderItem.getMenuId());
            if (menu.getStock() != null && menu.getStock() >= 0) {
                menu.setStock(menu.getStock() - orderItem.getQuantity());
                restaurantMenuMapper.updateById(menu);
            }
        }

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.RESTAURANT_ORDER_CREATED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                order.getStatus(),
                "新餐厅订单"));

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException("订单不存在");
        }
        if (!"restaurant".equals(order.getOrderType())) {
            throw new BusinessException("订单类型错误");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("订单状态错误");
        }
        order.setStatus("cooking");
        order.setPaidAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.RESTAURANT_ORDER_STATUS_CHANGED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                order.getStatus(),
                "餐厅订单已支付"));
    }

    @Override
    public List<RestaurantOrderVO> listMerchantOrders(Long merchantId, Long pondId, String status) {
        List<RestaurantOrderVO> orders = shopOrderMapper.selectMerchantRestaurantOrders(merchantId, pondId, status);
        fillItems(orders);
        return orders;
    }

    @Override
    public List<RestaurantOrderVO> listMyOrders(Long userId) {
        List<RestaurantOrderVO> orders = shopOrderMapper.selectMyRestaurantOrders(userId);
        fillItems(orders);
        return orders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long orderId, Long merchantId, String status) {
        ShopOrder order = shopOrderMapper.selectById(orderId);
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException("订单不存在");
        }
        if (!"restaurant".equals(order.getOrderType())) {
            throw new BusinessException("订单类型错误");
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("无权操作该订单");
        }
        String currentStatus = order.getStatus();
        String nextStatus = STATUS_FLOW.get(currentStatus);
        if (nextStatus == null || !nextStatus.equals(status)) {
            throw new BusinessException("状态流转错误");
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.updateById(order);

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.RESTAURANT_ORDER_STATUS_CHANGED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                order.getStatus(),
                "餐厅订单状态变更"));
    }

    private void fillItems(List<RestaurantOrderVO> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        for (RestaurantOrderVO order : orders) {
            List<ShopOrderItem> items = shopOrderItemMapper.selectByOrderId(order.getId());
            List<RestaurantOrderItemVO> itemVos = new ArrayList<>();
            for (ShopOrderItem item : items) {
                RestaurantOrderItemVO vo = new RestaurantOrderItemVO();
                vo.setId(item.getId());
                vo.setOrderId(item.getOrderId());
                vo.setMenuId(item.getMenuId());
                vo.setMenuName(item.getMenuName());
                vo.setPrice(item.getPrice());
                vo.setCookingMethod(item.getCookingMethod());
                vo.setQuantity(item.getQuantity());
                vo.setCreateTime(item.getCreateTime());
                itemVos.add(vo);
            }
            order.setItems(itemVos);
        }
    }

    private String generateOrderNo() {
        return ORDER_PREFIX + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }

    private List<CookingMethodDTO> parseCookingMethods(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CookingMethodDTO>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
