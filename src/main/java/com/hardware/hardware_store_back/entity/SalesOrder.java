package com.hardware.hardware_store_back.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_order") // 这会在 MySQL 里自动建一张销售流水表
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hardwareItemId; // 关联的五金工具 ID
    private String itemName;     // 工具名称（冗余存储方便查询）
    private Integer quantity;    // 购买数量

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice; // 订单总价

    // 订单状态：0=待店员确认，1=已完成(扣库存)，2=已取消
    private Integer status;

    private LocalDateTime createTime; // 下单时间

    // ==========================================
    // Getter 和 Setter
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHardwareItemId() { return hardwareItemId; }
    public void setHardwareItemId(Long hardwareItemId) { this.hardwareItemId = hardwareItemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}