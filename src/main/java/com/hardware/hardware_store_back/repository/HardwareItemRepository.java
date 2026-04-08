package com.hardware.hardware_store_back.repository;

import com.hardware.hardware_store_back.entity.HardwareItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface HardwareItemRepository extends JpaRepository<HardwareItem, Long> {
    // 【修改】改为根据名称、品牌、价格查找，实现“名称-品牌-价格”对应唯一 ID
    Optional<HardwareItem> findByNameAndBrandAndPrice(String name, String brand, BigDecimal price);
}