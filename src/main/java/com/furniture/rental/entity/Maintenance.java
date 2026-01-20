package com.furniture.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance")
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "furniture_id")
    private Furniture furniture;

    private String description;
    
    private BigDecimal cost;
    
    private LocalDate maintenanceDate;

    public Maintenance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Furniture getFurniture() { return furniture; }
    public void setFurniture(Furniture furniture) { this.furniture = furniture; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public LocalDate getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(LocalDate maintenanceDate) { this.maintenanceDate = maintenanceDate; }
}
