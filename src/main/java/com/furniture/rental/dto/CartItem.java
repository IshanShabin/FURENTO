package com.furniture.rental.dto;

import java.math.BigDecimal;

public class CartItem {
    private Long furnitureId;
    private String furnitureName;
    private BigDecimal pricePerMonth;
    private int quantity;
    private int rentalPeriodMonths;

    public CartItem() {
    }

    public CartItem(Long furnitureId, String furnitureName, BigDecimal pricePerMonth, int quantity,
            int rentalPeriodMonths) {
        this.furnitureId = furnitureId;
        this.furnitureName = furnitureName;
        this.pricePerMonth = pricePerMonth;
        this.quantity = quantity;
        this.rentalPeriodMonths = rentalPeriodMonths;
    }

    public Long getFurnitureId() {
        return furnitureId;
    }

    public void setFurnitureId(Long furnitureId) {
        this.furnitureId = furnitureId;
    }

    public String getFurnitureName() {
        return furnitureName;
    }

    public void setFurnitureName(String furnitureName) {
        this.furnitureName = furnitureName;
    }

    public BigDecimal getPricePerMonth() {
        return pricePerMonth;
    }

    public void setPricePerMonth(BigDecimal pricePerMonth) {
        this.pricePerMonth = pricePerMonth;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getRentalPeriodMonths() {
        return rentalPeriodMonths;
    }

    public void setRentalPeriodMonths(int rentalPeriodMonths) {
        this.rentalPeriodMonths = rentalPeriodMonths;
    }
}
