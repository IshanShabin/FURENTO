package com.furniture.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rental_item")
public class RentalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @ManyToOne
    @JoinColumn(name = "furniture_id")
    private Furniture furniture;

    private int quantity;
    
    private int rentalPeriodMonths; // How many months rented for
    
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal rentAmount; // Monthly rent * quantity * months
    
    // Deposit calculation logic can be in service, stored here simply or not
    
    public RentalItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Rental getRental() { return rental; }
    public void setRental(Rental rental) { this.rental = rental; }
    public Furniture getFurniture() { return furniture; }
    public void setFurniture(Furniture furniture) { this.furniture = furniture; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getRentalPeriodMonths() { return rentalPeriodMonths; }
    public void setRentalPeriodMonths(int rentalPeriodMonths) { this.rentalPeriodMonths = rentalPeriodMonths; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getRentAmount() { return rentAmount; }
    public void setRentAmount(BigDecimal rentAmount) { this.rentAmount = rentAmount; }
}
