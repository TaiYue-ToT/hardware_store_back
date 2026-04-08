package com.hardware.hardware_store_back.repository;

import com.hardware.hardware_store_back.entity.HardwareItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HardwareItemRepository extends JpaRepository<HardwareItem, Long> {
    // 👇 新增：根据名称、品牌、规格查找唯一商品
    Optional<HardwareItem> findByNameAndBrandAndModel(String name, String brand, String model);
}