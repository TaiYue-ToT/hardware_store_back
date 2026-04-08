package com.hardware.hardware_store_back.controller;

import com.hardware.hardware_store_back.entity.HardwareItem;
import com.hardware.hardware_store_back.repository.HardwareItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController // 告诉 Spring：这是一个可以接收网址请求的“接待员”
@RequestMapping("/api/hardware") // 给这个接待员安排一个专属的工作窗口地址
@CrossOrigin(origins = "*") // 【超级重点】允许跨域！因为以后我们的 Vue 前端和 Java 后端会在不同的端口运行，没有这行前端就拿不到数据。
public class HardwareItemController {

    @Autowired // 自动把我们刚才写的“数据库操作员”请过来干活
    private HardwareItemRepository repository;

    // 接口 1：获取所有五金工具
    // 当浏览器访问 GET http://localhost:8080/api/hardware/items 时，就会执行下面的代码
    @Autowired
    private com.hardware.hardware_store_back.service.AISearchService aiSearchService;
    @GetMapping("/items")
    public List<HardwareItem> getAllItems() {
        // 没错，就这一行代码！Spring 会自动去数据库查出所有数据，并转换成 JSON 发给前端
        return repository.findAll();
    }

    // 接口 3：AI 语义智能搜索
    @PostMapping("/semantic-search")
    public Map<String, Object> semanticSearch(@RequestBody Map<String, Object> payload) {
        // 从前端发来的数据里提取 "query" 字段，比如："我需要用来切割金属管的工具"
        String query = payload.getOrDefault("query", "").toString();
        // 限制默认返回 5 个
        int limit = payload.containsKey("limit") ? (int) payload.get("limit") : 5;

        if (query.trim().isEmpty()) {
            return Map.of("ok", false, "error", "搜索内容不能为空");
        }

        // 交给军师处理！
        return aiSearchService.search(query, limit);
    }
    // 接口 2：添加/更新工具 (自动去重逻辑)
    @PostMapping("/add")
    public HardwareItem addItem(@RequestBody HardwareItem item) {
        // 1. 清洗数据：去掉名称中的横杠和数字（防止店员手动输入带后缀的名字）
        String cleanName = item.getName().replaceAll("[-0-9]+", "").trim();
        item.setName(cleanName);

        // 2. 在数据库中寻找是否已有：名称、品牌、规格完全一致的商品
        Optional<HardwareItem> existingOpt = repository.findByNameAndBrandAndModel(
                item.getName(), item.getBrand(), item.getModel()
        );

        if (existingOpt.isPresent()) {
            // 3. 如果找到了，就在原有库存基础上增加，并更新价格和位置
            HardwareItem dbItem = existingOpt.get();
            dbItem.setStock(dbItem.getStock() + item.getStock());
            dbItem.setPrice(item.getPrice()); // 以最新的进货价为准
            dbItem.setLocation(item.getLocation()); // 更新最新货架位置
            return repository.save(dbItem);
        } else {
            // 4. 如果没找到，则是一个全新的商品，直接保存（JPA会自动生成新ID）
            return repository.save(item);
        }
    }
}