package com.hardware.hardware_store_back.repository;

import com.hardware.hardware_store_back.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    // 帮我们自动写好“根据状态查询订单”的 SQL 语句
    List<SalesOrder> findByStatusOrderByCreateTimeDesc(Integer status);
}