package com.hardware.hardware_store_back.repository;

// 👉 注意这里导入的路径也跟着改啦！
import com.hardware.hardware_store_back.entity.HardwareItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HardwareItemRepository extends JpaRepository<HardwareItem, Long> {

}