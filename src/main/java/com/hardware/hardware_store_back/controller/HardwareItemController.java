package com.hardware.hardware_store_back.controller;

import com.hardware.hardware_store_back.entity.HardwareItem;
import com.hardware.hardware_store_back.repository.HardwareItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hardware")
@CrossOrigin(origins = "*")
public class HardwareItemController {

    @Autowired
    private HardwareItemRepository repository;

    // 1. 获取所有工具
    @GetMapping("/items")
    public List<HardwareItem> getAllItems() {
        return repository.findAll();
    }

    // 2. 添加/更新工具 (已修正：不再删除数字和横杠)
    @PostMapping("/add")
    public HardwareItem addItem(@RequestBody HardwareItem item) {
        // 【修正】只去前后空格，保留名称中的数字和符号
        String cleanName = item.getName().trim();
        item.setName(cleanName);

        // 匹配逻辑：名称、品牌、规格完全一致才合并
        Optional<HardwareItem> existingOpt = repository.findByNameAndBrandAndModel(
                item.getName(), item.getBrand(), item.getModel()
        );

        if (existingOpt.isPresent()) {
            HardwareItem dbItem = existingOpt.get();
            dbItem.setStock(dbItem.getStock() + item.getStock());
            dbItem.setPrice(item.getPrice());
            dbItem.setLocation(item.getLocation());
            return repository.save(dbItem);
        } else {
            return repository.save(item);
        }
    }

    // 3. 【新增】修改价格接口 (对应前端的“💾 保存”)
    @PutMapping("/updatePrice/{id}")
    public Map<String, Object> updatePrice(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<HardwareItem> itemOpt = repository.findById(id);
        if (itemOpt.isPresent()) {
            HardwareItem item = itemOpt.get();
            // 将传入的 price 转为 BigDecimal
            item.setPrice(new java.math.BigDecimal(payload.get("price").toString()));
            repository.save(item);
            return Map.of("ok", true);
        }
        return Map.of("ok", false, "error", "找不到该商品");
    }

    // 4. 【新增】彻底删除接口 (对应前端的“🗑️ 删除”)
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteItem(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return Map.of("ok", true);
        }
        return Map.of("ok", false, "error", "商品不存在或已被删除");
    }
}