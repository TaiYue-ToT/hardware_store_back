package com.hardware.hardware_store_back.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hardware_item")
public class HardwareItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer stock;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String brand;
    private String model;
    private String location;

    // ==========================================
    // Getter 和 Setter
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}