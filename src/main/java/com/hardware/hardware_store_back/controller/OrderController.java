package com.hardware.hardware_store_back.controller;

import com.hardware.hardware_store_back.entity.HardwareItem;
import com.hardware.hardware_store_back.entity.SalesOrder;
import com.hardware.hardware_store_back.repository.HardwareItemRepository;
import com.hardware.hardware_store_back.repository.SalesOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private SalesOrderRepository orderRepository;

    @Autowired
    private HardwareItemRepository itemRepository;

    // 1. 顾客发起购买请求（生成待确认订单）
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> payload) {
        Long itemId = Long.valueOf(payload.get("itemId").toString());
        Integer quantity = (Integer) payload.get("quantity");

        Optional<HardwareItem> itemOpt = itemRepository.findById(itemId);
        if (itemOpt.isEmpty()) return Map.of("ok", false, "error", "商品不存在");

        HardwareItem item = itemOpt.get();

        // 关键逻辑：防超卖！如果库存不够，直接拒绝生成订单
        if (item.getStock() < quantity) {
            return Map.of("ok", false, "error", "库存不足！当前仅剩: " + item.getStock() + " 个");
        }

        // 生成订单
        SalesOrder order = new SalesOrder();
        order.setHardwareItemId(item.getId());
        order.setItemName(item.getName());
        order.setQuantity(quantity);
        order.setTotalPrice(item.getPrice().multiply(new BigDecimal(quantity)));
        order.setStatus(0); // 0 = 待审批
        order.setCreateTime(LocalDateTime.now());

        orderRepository.save(order);
        return Map.of("ok", true, "message", "订单已发送给店员，等待确认");
    }

    // 2. 店员查看所有“待确认”的订单
    @GetMapping("/pending")
    public List<SalesOrder> getPendingOrders() {
        return orderRepository.findByStatusOrderByCreateTimeDesc(0);
    }

    // 3. 店员点击“确认”，正式扣库存！
    @PostMapping("/approve/{orderId}")
    public Map<String, Object> approveOrder(@PathVariable Long orderId) {
        Optional<SalesOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return Map.of("ok", false, "error", "订单不存在");

        SalesOrder order = orderOpt.get();
        if (order.getStatus() != 0) return Map.of("ok", false, "error", "该订单已经被处理过啦");

        Optional<HardwareItem> itemOpt = itemRepository.findById(order.getHardwareItemId());
        if (itemOpt.isEmpty()) return Map.of("ok", false, "error", "商品不存在");

        HardwareItem item = itemOpt.get();

        // 再次确认库存（防止在此期间被别人买走）
        if (item.getStock() < order.getQuantity()) {
            return Map.of("ok", false, "error", "确认失败：当前库存(" + item.getStock() + ")已不足以支付该订单(" + order.getQuantity() + ")");
        }

        // 扣除库存并保存
        item.setStock(item.getStock() - order.getQuantity());
        itemRepository.save(item);

        // 更新订单状态为：已完成
        order.setStatus(1);
        orderRepository.save(order);

        return Map.of("ok", true, "message", "订单已确认，库存已扣除");
    }

    // 4. 店员点击“取消/驳回”订单
    @PostMapping("/cancel/{orderId}")
    public Map<String, Object> cancelOrder(@PathVariable Long orderId) {
        Optional<SalesOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            SalesOrder order = orderOpt.get();
            order.setStatus(2); // 2 = 已取消
            orderRepository.save(order);
            return Map.of("ok", true);
        }
        return Map.of("ok", false, "error", "订单不存在");
    }
}