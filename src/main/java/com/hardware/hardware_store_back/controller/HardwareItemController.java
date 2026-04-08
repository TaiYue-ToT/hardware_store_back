package com.hardware.hardware_store_back.controller;

import com.hardware.hardware_store_back.entity.HardwareItem;
import com.hardware.hardware_store_back.repository.HardwareItemRepository;
import com.hardware.hardware_store_back.service.AISearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hardware")
// 【重要修复】显式允许所有常用的请求方法，否则浏览器会拦截 DELETE 和 PUT
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class HardwareItemController {

    @Autowired
    private HardwareItemRepository repository;

    @Autowired
    private AISearchService aiSearchService;

    @GetMapping("/items")
    public List<HardwareItem> getAllItems() {
        return repository.findAll();
    }

    // ... 在 HardwareItemController.java 中找到 addItem 方法 ...

    @PostMapping("/add")
    public HardwareItem addItem(@RequestBody HardwareItem item) {
        // 1. 去掉空格
        String cleanName = item.getName() != null ? item.getName().trim() : "";
        item.setName(cleanName);

        // 2. 匹配：名称、品牌、价格一致
        Optional<HardwareItem> existingOpt = repository.findByNameAndBrandAndPrice(
                item.getName(), item.getBrand(), item.getPrice()
        );

        if (existingOpt.isPresent()) {
            HardwareItem dbItem = existingOpt.get();
            // 增加库存
            dbItem.setStock(dbItem.getStock() + item.getStock());

            // 【关键修复点】只有当新传入的位置不为空时，才更新位置
            // 这样当你合并已有商品时，dbItem 会保留它原本的货架信息
            if (item.getLocation() != null && !item.getLocation().trim().isEmpty()) {
                dbItem.setLocation(item.getLocation());
            }

            return repository.save(dbItem);
        } else {
            // 全新商品，直接保存
            return repository.save(item);
        }
    }

    // 【修正】改价接口
    @PutMapping("/updatePrice/{id}")
    public Map<String, Object> updatePrice(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<HardwareItem> itemOpt = repository.findById(id);
        if (itemOpt.isPresent()) {
            HardwareItem item = itemOpt.get();
            item.setPrice(new BigDecimal(payload.get("price").toString()));
            repository.save(item);
            return Map.of("ok", true);
        }
        return Map.of("ok", false, "error", "找不到该商品");
    }

    // 【修正】删除接口
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteItem(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return Map.of("ok", true);
        }
        return Map.of("ok", false, "error", "商品不存在或已被删除");
    }

    @PostMapping("/semantic-search")
    public Map<String, Object> semanticSearch(@RequestBody Map<String, Object> payload) {
        String query = payload.getOrDefault("query", "").toString();
        int limit = payload.containsKey("limit") ? (int) payload.get("limit") : 5;
        if (query.trim().isEmpty()) return Map.of("ok", false, "error", "搜索内容不能为空");
        return aiSearchService.search(query, limit);
    }
}