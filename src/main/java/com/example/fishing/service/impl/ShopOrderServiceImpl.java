package com.example.fishing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fishing.common.BusinessException;
import com.example.fishing.dto.ShopOrderCreateDTO;
import com.example.fishing.dto.ShopOrderQuery;
import com.example.fishing.entity.Pond;
import com.example.fishing.entity.ShopOrder;
import com.example.fishing.entity.ShopOrderItem;
import com.example.fishing.entity.ShopProduct;
import com.example.fishing.entity.SysUser;
import com.example.fishing.mapper.PondMapper;
import com.example.fishing.mapper.ShopOrderItemMapper;
import com.example.fishing.mapper.ShopOrderMapper;
import com.example.fishing.mapper.SysUserMapper;
import com.example.fishing.notify.NotificationEvent;
import com.example.fishing.notify.NotificationEventType;
import com.example.fishing.service.ShopOrderService;
import com.example.fishing.service.ShopProductService;
import com.example.fishing.vo.ShopOrderItemVO;
import com.example.fishing.vo.ShopOrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 商城订单服务实现
 */
@Service
public class ShopOrderServiceImpl extends ServiceImpl<ShopOrderMapper, ShopOrder> implements ShopOrderService {

    @Autowired
    private ShopProductService shopProductService;

    @Autowired
    private ShopOrderItemMapper shopOrderItemMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PondMapper pondMapper;

    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopOrderVO createOrder(Long userId, ShopOrderCreateDTO dto) {
        if (CollectionUtils.isEmpty(dto.getItems())) {
            throw new BusinessException("订单商品不能为空");
        }

        List<Long> productIds = dto.getItems().stream()
                .map(ShopOrderCreateDTO.Item::getProductId)
                .distinct()
                .collect(Collectors.toList());

        List<ShopProduct> products = shopProductService.listByIds(productIds);
        Map<Long, ShopProduct> productMap = products.stream()
                .collect(Collectors.toMap(ShopProduct::getId, p -> p));

        if (productMap.size() != productIds.size()) {
            throw new BusinessException("存在无效商品");
        }

        Long merchantId = null;
        Long pondId = dto.getPondId();
        int totalAmount = 0;
        List<ShopOrderItem> orderItems = new ArrayList<>();

        for (ShopOrderCreateDTO.Item item : dto.getItems()) {
            ShopProduct product = productMap.get(item.getProductId());
            if (!"on".equals(product.getStatus())) {
                throw new BusinessException("商品[" + product.getName() + "]已下架");
            }
            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new BusinessException("商品[" + product.getName() + "]库存不足");
            }
            if (merchantId == null) {
                merchantId = product.getMerchantId();
            } else if (!merchantId.equals(product.getMerchantId())) {
                throw new BusinessException("一次下单只能选择同一商家的商品");
            }
            if (pondId == null) {
                pondId = product.getPondId();
            }

            int subtotal = product.getPrice() * item.getQuantity();
            totalAmount += subtotal;

            ShopOrderItem orderItem = new ShopOrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);
        }

        // 扣减库存
        for (ShopOrderCreateDTO.Item item : dto.getItems()) {
            if (!shopProductService.deductStock(item.getProductId(), item.getQuantity())) {
                ShopProduct product = productMap.get(item.getProductId());
                throw new BusinessException("商品[" + product.getName() + "]库存不足或已下架");
            }
        }

        String orderNo = generateOrderNo();
        LocalDateTime now = LocalDateTime.now();

        ShopOrder order = new ShopOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setPondId(pondId);
        order.setOrderType("shop");
        order.setTotalAmount(totalAmount);
        order.setStatus("pending_pay");
        order.setCreateTime(now);
        order.setUpdateTime(now);
        baseMapper.insert(order);

        for (ShopOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setCreateTime(now);
            shopOrderItemMapper.insert(item);
        }

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SHOP_ORDER_CREATED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                order.getStatus(),
                "新商城订单"));

        return buildOrderVO(order, orderItems, productMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId) {
        ShopOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        if (!"pending_pay".equals(order.getStatus())) {
            throw new BusinessException("订单状态异常");
        }
        LambdaUpdateWrapper<ShopOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShopOrder::getId, orderId);
        wrapper.set(ShopOrder::getStatus, "paid");
        wrapper.set(ShopOrder::getPaidAt, LocalDateTime.now());
        wrapper.set(ShopOrder::getUpdateTime, LocalDateTime.now());
        baseMapper.update(null, wrapper);

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SHOP_ORDER_STATUS_CHANGED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                "paid",
                "商城订单已支付"));
    }

    @Override
    public IPage<ShopOrderVO> myOrders(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrder::getUserId, userId);
        wrapper.eq(ShopOrder::getOrderType, "shop");
        wrapper.orderByDesc(ShopOrder::getCreateTime);
        IPage<ShopOrder> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public IPage<ShopOrderVO> queryPage(Long merchantId, ShopOrderQuery query) {
        LambdaQueryWrapper<ShopOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(merchantId != null, ShopOrder::getMerchantId, merchantId);
        wrapper.eq(ShopOrder::getOrderType, "shop");
        wrapper.eq(query.getStatus() != null && !query.getStatus().isEmpty(), ShopOrder::getStatus, query.getStatus());
        wrapper.orderByDesc(ShopOrder::getCreateTime);
        IPage<ShopOrder> page = baseMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long merchantId, Long orderId, String status) {
        ShopOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (merchantId != null && !merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("无权操作该订单");
        }
        if (!("completed".equals(status) || "cancelled".equals(status))) {
            throw new BusinessException("不支持的目标状态");
        }
        if ("completed".equals(status) && !"paid".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可标记为完成");
        }
        if ("cancelled".equals(status) && !("pending_pay".equals(order.getStatus()) || "paid".equals(order.getStatus()))) {
            throw new BusinessException("当前状态不可取消");
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(order);

        eventPublisher.publishEvent(NotificationEvent.of(NotificationEventType.SHOP_ORDER_STATUS_CHANGED,
                order.getMerchantId(),
                order.getPondId(),
                order.getId(),
                status,
                "商城订单状态已更新"));
    }

    private ShopOrderVO convertToVO(ShopOrder order) {
        LambdaQueryWrapper<ShopOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopOrderItem::getOrderId, order.getId());
        wrapper.orderByAsc(ShopOrderItem::getId);
        List<ShopOrderItem> items = shopOrderItemMapper.selectList(wrapper);

        Map<Long, ShopProduct> productMap = items.isEmpty() ? java.util.Collections.emptyMap()
                : shopProductService.listByIds(items.stream().map(ShopOrderItem::getProductId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(ShopProduct::getId, p -> p));

        return buildOrderVO(order, items, productMap);
    }

    private ShopOrderVO buildOrderVO(ShopOrder order, List<ShopOrderItem> items, Map<Long, ShopProduct> productMap) {
        ShopOrderVO vo = new ShopOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setMerchantId(order.getMerchantId());
        vo.setPondId(order.getPondId());
        vo.setOrderType(order.getOrderType());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPaidAt(order.getPaidAt());

        if (order.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(order.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserNickname(user.getNickname());
            }
        }
        if (order.getPondId() != null) {
            Pond pond = pondMapper.selectById(order.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getName());
            }
        }

        List<ShopOrderItemVO> itemVOs = new ArrayList<>();
        for (ShopOrderItem item : items) {
            ShopOrderItemVO itemVO = new ShopOrderItemVO();
            itemVO.setId(item.getId());
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            ShopProduct product = productMap.get(item.getProductId());
            itemVO.setProductImageUrl(product != null ? product.getImageUrl() : null);
            itemVO.setQuantity(item.getQuantity());
            itemVO.setUnitPrice(item.getUnitPrice());
            itemVO.setSubtotal(item.getSubtotal());
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(ORDER_TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "shop_" + time + random;
    }
}
