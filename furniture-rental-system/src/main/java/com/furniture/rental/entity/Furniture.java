package com.furniture.rental.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "furniture")
public class Furniture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    @Size(max = 255, message = "Name must be 255 characters or fewer.")
    @Column(nullable = false)
    private String name;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer.")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Rental price is required.")
    @DecimalMin(value = "0.01", message = "Rental price must be greater than zero.")
    @Digits(integer = 8, fraction = 2, message = "Rental price can have at most 2 decimal places.")
    @Column(nullable = false)
    private BigDecimal rentalPricePerMonth;

    @DecimalMin(value = "0.00", message = "Buy price cannot be negative.")
    @Digits(integer = 8, fraction = 2, message = "Buy price can have at most 2 decimal places.")
    private BigDecimal purchasePrice;

    @Min(value = 0, message = "Stock cannot be negative.")
    private int stockQuantity;

    private boolean available; // true if stock > 0

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "furniture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FurnitureImage> images;

    @OneToMany(mappedBy = "furniture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews;

    public Furniture() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getRentalPricePerMonth() {
        return rentalPricePerMonth;
    }

    public void setRentalPricePerMonth(BigDecimal rentalPricePerMonth) {
        this.rentalPricePerMonth = rentalPricePerMonth;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        this.available = stockQuantity > 0;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<FurnitureImage> getImages() {
        return images;
    }

    public void setImages(List<FurnitureImage> images) {
        this.images = images;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
}
